package com.ywh.plugin.checkbase;

public class CheckBaseExtension {

    //默认有不满足条件时抛异常
    public boolean throwException = true;
    //有没有继承指定Activity后，抛异常的时机, 1有一个马上抛，2 检查完之后抛
//    public int exceptionOpportunity = 1;

    //有一个立马抛异常
    public boolean exceptionAtOnce = true;
    //打印日志
    public boolean openLog;
}
