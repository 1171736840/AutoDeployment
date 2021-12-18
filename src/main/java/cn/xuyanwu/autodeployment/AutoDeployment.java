package cn.xuyanwu.autodeployment;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.intellij.openapi.project.Project;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class AutoDeployment {
    private final String basePath;
    private final File file;
    private final Log log;
    private Session session;
    private ChannelShell channel;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isClose;


    public AutoDeployment(Project project,File file,Log log) {
        this.basePath = project.getBasePath();
        this.file = file;
        this.log = log;
    }

    public void deployment() {
        if (file == null || (!file.exists()) || (!file.isFile())) {
            log.println("没有找到配置文件！");
            return;
        }
        new Thread(() -> {
            log.println("开始自动部署...");
            try {
                String extName = FileNameUtil.extName(file);
                if ("json".equals(extName)) {
                    deploymentByJSON(file);
                } else if ("sh".equals(extName)) {
                    deploymentBySH(file);
                } else {
                    log.println("不支持 " + extName + " 类型的文件！");
                }
            } catch (Exception e) {
                log.println("自动部署失败！" + e.getLocalizedMessage());
                e.printStackTrace();
                close();
            }
        }).start();
    }

    /**
     * 通过 sh 脚本方式部署
     */
    private void deploymentBySH(File file) throws Exception {

        // 读取命令及配置
        String text = FileUtil.readString(file,StandardCharsets.UTF_8);
        if (StrUtil.isBlank(text)) {
            log.println(file.getName() + " 文件是空的！");
            return;
        }

        String[] lines = text.split("\n");

        String host = findConfigBySH(lines,"# host =");
        int port;
        try {
            String p = findConfigBySH(lines,"# port =");
            if (p == null) {
                log.println("没有找到 port 的值！");
                return;
            }
            port = Integer.parseInt(p);

        } catch (Exception e) {
            log.println("port 的值不是数字！");
            return;
        }
        String username = findConfigBySH(lines,"# username =");
        String password = findConfigBySH(lines,"# password =");
        String remoteDir = findConfigBySH(lines,"# remoteDir =");   //上传到的远程文件夹
        List<String> localFiles = findConfigsBySH(lines,"# localFile =").stream().filter(StrUtil::isNotBlank).collect(Collectors.toList());   //本地要上传的文件，相对于项目的路径

        if (StrUtil.isBlank(host)) {
            log.println("没有找到 host 的值！");
            return;
        }
        if (StrUtil.isBlank(username)) {
            log.println("没有找到 username 的值！");
            return;
        }
        if (StrUtil.isBlank(password)) {
            log.println("没有找到 password 的值！");
            return;
        }
        if (StrUtil.isBlank(remoteDir)) {
            log.println("没有找到 remoteDir 的值！");
            return;
        }
        if (localFiles.isEmpty()) {
            log.println("没有找到 localFile 的值！但是部署还在继续...");
        }

        Config config = new Config(host,port,username,password);

        session = LinuxConnetionHelper.connect(config);
        channel = LinuxConnetionHelper.openChannelShell(session);
        channel.connect();
        inputStream = channel.getInputStream();
        outputStream = channel.getOutputStream();

        autoPrintHostLog();

        //开始上传文件

        for (String filename : localFiles) {
            log.println("开始上传文件：" + filename);
            String localFilename = filename.substring(filename.lastIndexOf("/") + 1);
            String tempFilename = remoteDir + localFilename;
            LinuxConnetionHelper.uploadFile(session,basePath + filename,tempFilename,log);
            log.println("上传文件完成：" + filename);
        }

        writeln("set +o history");  //不记录当前会话的历史命令

        log.println("开始执行 sh 脚本：");
        StringBuilder sb = new StringBuilder();
        //开始执行每条命令
        for (String line : lines) {
            //格式化命令
            if (line.trim().startsWith("#")) continue;
            sb.append(line.concat("\n"));
        }
        writeln(sb.toString());

    }


    /**
     * 查找 sh 中的自定义配置文件
     */
    private String findConfigBySH(String[] lines,String prefix) {
        List<String> configs = findConfigsBySH(lines,prefix);
        if (configs.size() == 0) {
            return null;
        }
        return configs.get(0);
    }

    /**
     * 查找 sh 中的自定义配置文件
     */
    private List<String> findConfigsBySH(String[] lines,String prefix) {
        ArrayList<String> list = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith(prefix)) {
                list.add(line.substring(prefix.length()).trim());
            }
        }
        return list;
    }

    private void autoPrintHostLog() {
        //输出日志
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream,StandardCharsets.UTF_8))) {
                String msg;
                while (!isClose && (msg = in.readLine()) != null) {
                    log.println(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 通过 json 配置文件方式部署
     */
    private void deploymentByJSON(File file) throws Exception {
        String text = FileUtil.readString(file,StandardCharsets.UTF_8);
        List<Config> configList = JSON.parseArray(text,Config.class);
        if (configList.isEmpty()) {
            log.println("autoDeployment.json 中没有配置远程主机！");
            return;
        }
        Config config = configList.get(0);
        session = LinuxConnetionHelper.connect(config);
        channel = LinuxConnetionHelper.openChannelShell(session);
        channel.connect();
        inputStream = channel.getInputStream();
        outputStream = channel.getOutputStream();

        autoPrintHostLog();


        writeln("set +o history");  //不记录当前会话的历史命令

        String backupPath = "/www/backup/AutoDeployment/";
        String tempPath = "/tmp/AutoDeployment/";
        String localFilename = config.getLocalFile().substring(config.getLocalFile().lastIndexOf("/") + 1);
        String remoteFilename = config.getRemoteFile().substring(config.getRemoteFile().lastIndexOf("/") + 1);
        String datetime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        String tempFilename = tempPath + datetime + "." + localFilename;

        LinuxConnetionHelper.uploadFile(session,basePath + config.getLocalFile(),tempFilename,log);
        log.println("文件上传完成！");
        writeln(config.getStopCMD());   //停止服务器


        //备份原文件
        writeln("mkdir -p " + backupPath);
        String suffix = config.getRemoteFile().substring(config.getRemoteFile().lastIndexOf("."));
        writeln(String.format("mv -f %s %s",config.getRemoteFile(),backupPath + datetime + remoteFilename));
        writeln(String.format("mv -f %s %s",
                config.getRemoteFile().substring(0,config.getRemoteFile().length() - suffix.length()),
                (backupPath + datetime + "." + remoteFilename).substring(0,(backupPath + datetime + "." + remoteFilename).length() - suffix.length()))
        );

        //将临时文件移动到指定位置
        writeln(String.format("mv -f %s %s",tempFilename,config.getRemoteFile()));


        writeln(config.getStartCMD() + " & echo 自动部署已完成，开始输出启动日志 && " + config.getLogCMD());   //停止服务器并查看日志
    }

    private void writeln(String cmd) throws IOException {
        if (outputStream != null && !isClose) {
            outputStream.write(cmd.concat("\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
    }

    /**
     * 关闭对象
     */
    public void close() {
        isClose = true;
        IoUtil.close(inputStream);
        inputStream = null;
        IoUtil.close(outputStream);
        outputStream = null;
        LinuxConnetionHelper.closeChannelShell(channel);
        channel = null;
        LinuxConnetionHelper.close(session);
        session = null;
    }
}
