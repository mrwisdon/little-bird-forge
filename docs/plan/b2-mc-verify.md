# B-2 进 battlefield 模组 · 独立验收报告（mc-verify / task-mc3）

只认文件与命令输出（不引用他人结论）。复现命令：python3 tools/_b2verify_checks.py / _b2verify_uvcolor.py / _b2verify_extra.py / _b2verify_preview.py（均在仓库 tools/ 下）。
按 Lead 分工 **未跑 gradle**；blend 对拍的限制见第 8 条。

1. **b2_mc.json** parts=174（sha256 38160998…）；scale=11.4832535885 units/m、16 units/block；bbox x±296 / y 0..64 / z±120 units → 长 **20.900 m**（契约 20.85±0.05，正好上界）PASS；半展 **25.777 m** vs 26.18±0.05 → **FAIL（−0.403 m）**；顶高 **5.573 m** vs ≤4.88 → **FAIL（+0.693 m，格点吸附）**；轮底 0.000 PASS；鼻/尾 ±120.00 PASS。
2. **b2_preview.obj / .mtl** v=17473 vt=13 vn=49218 f=31652 tris=**31652**、非有限坐标 0、bbox **20.848×5.167×52.360 m**（y_min −0.004）、mtllib=b2_preview.mtl、map_Kd=battlefield:item/b2_preview（目标贴图存在）、UV 13/13 精确落格心（worst **0.00000**）→ PASS；与 b2_mc.json 米制对比 span **+1.56%** / up **−7.29%** → **FAIL（体素盒≠网格）**。
3. **UV↔贴图（flip_v=true 硬判据）** 行映射唯一可证（mirror 16/16 命中 vs identity 6/16）；逐 vt 采样 13/13 与其契约材质一致：vt3 skin #26292E、vt0 panel #22252A、vt4 glass #1B2A24、vt11 intake #0E0F11、vt5 exhaust #34383C、vt1 metal #8A9096、vt10 gear_white #D8DAD6、vt2 bay_white #C8CBC6、vt9 tire #17181A、vt12 ordnance #55584A、vt7 light_r #C03028、vt6 light_g #2FA84F、vt8 light_w #E8E6D8；面积加权 **90.8%** 落在深色蒙皮、0/31652 三角形跨格 → **PASS**。
4. **贴图** b2.png 512² pow2 sha256 c85ed948…；b2_preview.png 256² pow2 sha256 608248c7…；16 格中心色互异且语义正确；但 **16 格非平色（45516/65536 像素 ≠ 格心色）** → 与契约「ABGR 平色」字面 **FAIL**（不影响材质识别）。b2.png 7 个 texOffs 区均值：(0,0)#2C2F34×230、(0,144)#3B3E3F×20、(0,208)#272124×5、(128,144)#8E949A×6、(256,0)#24272C×14、(256,144)#9B9E9B×4、(448,0)#131417×42 → 深色 5 区 + 低饱和金属/白 2 区 PASS。
5. **B2Model.java** addBox()=**321**（children=322）vs json parts=174 → 字面 **FAIL（321≠174）**；但并集 bbox 与 json 逐轴一致（[−296,0,−120]..[296,64,120]）、总体积 **1440256.0 units³ delta 0.000**、抽 3 盒 box_0000 / box_0087 / box_0173 的 bboxdiff **0.0000**、voldiff **0.000** → **切段无损（等价 PASS）**；321 cube 共用 7 个 texOffs。
6. **B2Geometry 常量** NOSE −120.0 / TAIL 120.0 / WING_TIP 300.87 / ROOF_TOP 56.0 / BELLY 17.1 / SKID 0.0、UNITS_PER_BLOCK=16.0 全符契约 PASS；但 box 模型 max|x|=**296** vs 声明 300.87 → **FAIL（−4.87 units = −0.424 m/侧）**。
7. **注册与资源** ModEntities register("b2")×1（+B2Entity/B2Geometry import）、ModItems register("b2_preview")×1、ModCreativeTabs B2/B2_PREVIEW 各 1、ClientModEvents B2Model.B2_LAYER×1 + renderer=**B2ObjRenderer**（主视觉 OBJ 已接）、无重复 register id；12 个必需文件全在；b2_preview.json loader=forge:obj flip_v=true、b2.json → textures/item/b2.png；lang en/zh 各 3 key → **PASS**。插入纪律：git diff 删除行 **0**（ModEntities +58、ModItems +55、ModCreativeTabs +7、ClientModEvents +22；B2 相关删除行 0）；JetType.java 为**未跟踪新文件**（HEAD 中不存在，与 JetConfig/F35Profile/Su57Profile/JetAxes/JetState/JetInput/JetFlightModel 等一并新增），其 B2 枚举项随该新文件落盘，非对已跟踪文件的插入；B2 文件均为新增（untracked）。
8. **未做 / 受限** ① ./gradlew build 按 Lead 分工由 Lead 跑（待复核 BUILD 结果 + JAR 大小/SHA-256 + build/libs 仅 1 个 battlefield JAR）；② B2_assembled.blend 对拍：WSL Blender 4.0.2 报 not a blend file，unzstd 后头部为 BLENDER17-01（非标准 BLENDER-vXXX），需 Windows 侧同版本 Blender；OBJ 为直接校验，不依赖该对拍。

**FAIL 汇总（待 Lead 决策）** ① 体素盒半展只到 296 units → box 渲染器/实体 AABB 比 B2Geometry 声明窄 0.424 m/侧（OBJ 主视觉不受影响）；② 顶高：json 64 units (5.573 m)、网格实测 5.167 m，均超契约 4.88 m；③ 「cube 数 = parts 数」判据建议改为「并集 bbox + 体积等式」（已证等价）；④ 预览贴图非平色（建议契约措辞改为「格心/中位色」）。

交付物：本报告 + docs/plan/b2-mc-verify-preview.png（880×1075，上=侧视 / 下=俯视，鼻朝右，竖线=1 block）+ 4 个验证脚本 tools/_b2verify_checks.py、_b2verify_uvcolor.py、_b2verify_extra.py、_b2verify_preview.py。
