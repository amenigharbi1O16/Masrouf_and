package com.example.masrouf1;

/** Result of a repository command, including a stable validation or persistence code. */
public final class OperationResult<T> {
    public final T value;
    public final String error;
    private OperationResult(T value,String error){this.value=value;this.error=error;}
    public static <T> OperationResult<T> success(T value){return new OperationResult<>(value,null);}
    public static <T> OperationResult<T> failure(String error){return new OperationResult<>(null,error);}
    public boolean succeeded(){return error==null;}
}
