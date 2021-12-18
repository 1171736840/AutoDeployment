package cn.xuyanwu.autodeployment;

public interface Log {
    void print(String str);

    default void println(String str) {
        print(str + "\n");
    }
}
