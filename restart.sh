#!/bin/bash

# 微信小程序后端服务重启脚本
# 使用方法: ./restart.sh

APP_NAME="wx.jar"
LOG_FILE="wx.log"


echo "========== 开始重启 $APP_NAME =========="
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"

# 查找并停止现有进程
echo "正在查找 $APP_NAME 进程..."
pid=$(ps -ef | grep "$APP_NAME" | grep -v grep | awk '{print $2}')

if [ -n "$pid" ]; then
    echo "找到进程 PID: $pid"
    echo "正在停止进程..."
    kill -15 $pid  # 优雅停止
    sleep 3
    
    # 检查进程是否还存在
    if ps -p $pid > /dev/null 2>&1; then
        echo "优雅停止失败，强制终止进程..."
        kill -9 $pid
        sleep 2
    fi
    
    if ps -p $pid > /dev/null 2>&1; then
        echo "错误: 进程停止失败"
        exit 1
    else
        echo "进程已成功停止"
    fi
else
    echo "未找到运行中的 $APP_NAME 进程"
fi

# 检查jar文件是否存在
if [ ! -f "$APP_NAME" ]; then
    echo "错误: $APP_NAME 文件不存在"
    exit 1
fi

# 备份旧日志
if [ -f "$LOG_FILE" ]; then
    mv "$LOG_FILE" "${LOG_FILE}.$(date +%Y%m%d_%H%M%S).bak"
fi

# 启动应用
echo "正在启动 $APP_NAME..."
nohup java -jar wx.jar  >/dev/null 2>&1 &
new_pid=$!

# 等待启动
echo "等待应用启动..."
sleep 5

# 检查启动状态
if ps -p $new_pid > /dev/null 2>&1; then
    echo "========== 启动成功 =========="
    echo "进程 PID: $new_pid"
    echo "日志文件: $LOG_FILE"
    echo "查看日志: tail -f $LOG_FILE"
else
    echo "========== 启动失败 =========="
    echo "请检查日志: cat $LOG_FILE"
    exit 1
fi

echo "重启完成时间: $(date '+%Y-%m-%d %H:%M:%S')"
exit 0
