# GNSSLogger_dual
forked from: https://github.com/superhang/GNSSLogger

GNSSLoggerR v0.0.1a

对原创的GNSSLogger实施了RINEX输出，传感器输出，各种记录功能追加等的改良的版本。

现阶段的功能如下。



代码伪距离和载波相位计算

气压、磁、加速度传感器的值的取得、以及使用这些的终端的角度计算

磁罗盘中地磁与正北的偏差（磁偏角）的计算及其校正

RINEX ver2.11的输出

RINEX ver3.03的输出

L1频带和L5频带的2频带的RINEX的输出



更新内容一览

将Pseudorange Smoother的适用范围扩大到GLONASS、QZSS.

对应NMEA的输出.

添加以秒为单位设定观测时间的功能。

修正Huawei Honor 8中的错误.

修正SkyPlot的显示错误.



ver 1.3更新内容一览

对应L1频带和L5频带的双频观测

2周波观测的选择可能





-------------------

# GNSS_FUSING

Gong added：

1、修复了一些BUG

2、添加了加速度计等Sensors的原始输出

3、修复了kml文件格式的输出



To Do：

1、rinex输出尚不支持BDS，疑似bug：信号频率的区分：

- 似乎北斗只支持B1信号

2、SQL数据库入库功能完善

3、加入高德地图API

4、算法融合

5、LOG文件生成



