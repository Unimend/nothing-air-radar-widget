# 空气质量雷达

Nothing 点阵风格的原生 Android 1×1 桌面组件。首页以四根点阵柱显示 PM2.5、PM10、O₃ 与 NO₂：柱高使用各污染物专项 US AQI，柱下数字显示真实浓度；第二页显示未来24小时 US AQI 模型趋势、峰值时段与主导污染物。

![空气质量雷达组件实机截图](screenshots/widget.png)

- 数据：Open-Meteo Air Quality API / CAMS 模型，无 API Token。
- 明确显示 `US AQI · MODEL`，不将模型结果冒充中国官方 AQI 或地面监测站实测。
- 每个组件实例独立保存城市、缓存和页码。
- 刷新与切页播放约1.5秒雷达扫描，静止后停止高频更新。
- Doto 用于英文和数字，Zpix 用于中文；纯黑底、14dp 圆角及 3×5 对齐遵循本工作区组件规范。

## 构建

```sh
sh build.sh
adb install -r build/air-radar.apk
```

安装后打开“空气质量雷达”选择城市，再手动添加1×1组件。主体点击打开设置，右侧48dp热区切换雷达页和24小时趋势页。

权限仅包含联网、网络状态和开机恢复任务。数据来源：https://open-meteo.com/en/docs/air-quality-api
