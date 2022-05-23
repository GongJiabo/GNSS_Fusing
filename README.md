# GNSS_FUSING
### 在Github开源项目上进行二次开发：

https://github.com/superhang/GNSSLogger

https://github.com/butterflying10/gnssdatalogger



### 安卓端GNSS/PDR定位数据集与Matlab代码：

链接：https://pan.baidu.com/s/1FGgyDvH54JCuoxWgZTMz0A?pwd=qffp 
提取码：qffp 

![code](.\pic\code.png)

-------------------

### 安卓软件GNSS_Fusion

现存BUG：

1、接收广播星历只支持GPS

2、定位功能中Ntrip协议功能不全，无法使用

3、添加的定位功能似乎存在bug，且只有SPP

---------------------------------

Gong added：

1、修复了一些BUG

2、添加了加速度计等Sensors的原始输出

3、修复了kml文件格式的输出

4、基本功能已经完善，六大输出模块

- CSV - 原始传感器数据
- KML - Goole Earth可以直接读取的坐标文件
- NMEA - 原始GNSS观测数据
- RAWDATA - 与GnssLogger输出的log同格式
- RINEXNAV - RINEX格式导航星历文件（仅GPS）
- RINEXOBS - RINEX格式GNSS观测文件



To Do：

1、~~SQL数据库入库功能完善~~

2、~~加入高德地图API~~

**3、算法融合**：

- PDR/GNSS组合算法
- 伪距差分定位
- PPP等

4、LOG文件生成

5、稳定性测试

------------------

App View:

![data_status](.\pic\data_status.jpg)

![pos_settings_all](.\pic\pos_settings_all.jpg)
