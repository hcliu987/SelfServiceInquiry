#!/bin/bash
echo "************ 查找进程 **************"
pid=`ps -ef | grep "wx.jar" | grep -v grep | awk '{print $2}'`
if [ -n "$pid" ]; then
        echo $pid Manager
        kill -9 $pid
        if [ "$?" -eq 0 ]; then
                echo "kill success"
        else
                echo "kill failed"
        fi
        echo "************ 杀掉进程 **************"
fi
nohup java -jar wx.jar > wx.log 2>&1 &
echo "************ 启动成功 **************"
exit 0
