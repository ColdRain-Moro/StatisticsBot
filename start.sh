#!/bin/bash

# 不生成随机设备信息了，直接手动获取扫码的 device.json
python3 ./update_device.py
java -jar StatisticsBot-1.0-SNAPSHOT-all.jar