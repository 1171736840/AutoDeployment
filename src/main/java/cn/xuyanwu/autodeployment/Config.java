package cn.xuyanwu.autodeployment;


public class Config {
    /*名称*/
    private String name;
    /*主机地址*/
    private String host;
    /*主机端口*/
    private Integer port;
    /*登录账户*/
    private String user;
    /*登录密码*/
    private String password;
    /*本地 war 包地址，相对 target 目录*/
    private String localFile;
    /*远程 war 包地址，相对根目录*/
    private String remoteFile;
    /* 启动命令 */
    private String startCMD;
    /* 停止命令 */
    private String stopCMD;
    /* 查看日志命令 */
    private String logCMD;

    public Config() {
    }

    public Config(String host,Integer port,String user,String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocalFile() {
        return localFile;
    }

    public void setLocalFile(String localFile) {
        this.localFile = localFile;
    }

    public String getRemoteFile() {
        return remoteFile;
    }

    public void setRemoteFile(String remoteFile) {
        this.remoteFile = remoteFile;
    }

    public String getStartCMD() {
        return startCMD;
    }

    public void setStartCMD(String startCMD) {
        this.startCMD = startCMD;
    }

    public String getStopCMD() {
        return stopCMD;
    }

    public void setStopCMD(String stopCMD) {
        this.stopCMD = stopCMD;
    }

    public String getLogCMD() {
        return logCMD;
    }

    public void setLogCMD(String logCMD) {
        this.logCMD = logCMD;
    }
}
