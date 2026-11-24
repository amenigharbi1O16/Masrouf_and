package com.example.masrouf1;

/** Loading, success, or error state exposed to dashboard views. */
public final class DataState<T> {
    public final boolean loading;
    public final T data;
    public final boolean failed;
    private DataState(boolean loading,T data,boolean failed){this.loading=loading;this.data=data;this.failed=failed;}
    public static <T> DataState<T> loading(){return new DataState<>(true,null,false);}
    public static <T> DataState<T> success(T data){return new DataState<>(false,data,false);}
    public static <T> DataState<T> failure(){return new DataState<>(false,null,true);}
}
