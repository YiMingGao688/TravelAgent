package com.itheima.travelagent2.common;

// 手动写一个无Lombok的Result类，完全可编译
public class Result<T> {
    private int code;
    private String message;
    private T data;

    // 无参构造（必须）
    public Result() {}

    // 全参构造（必须，解决new Result(...)报错）
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // Getter/Setter（解决字段未使用警告）
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    // 静态成功方法，带泛型，解决控制器类型不匹配
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // 静态失败方法
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}