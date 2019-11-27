package com.example.event;



public interface IMessageEvent
{
	public void onReceiveMessage(String className, Object data);
}
