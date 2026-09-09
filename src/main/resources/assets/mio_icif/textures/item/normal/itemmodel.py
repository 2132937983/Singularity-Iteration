import os

# 获取当前脚本的文件名（用于排除自身）
current_script = os.path.basename(__file__)
# 定义输出的txt文件名
output_file = "items_code.txt"
# 存储生成的代码行
code_lines = []

# 遍历当前目录下的所有文件
for filename in os.listdir("."):
    # 排除当前脚本文件和输出的txt文件
    if filename == current_script or filename == output_file:
        continue
    
    # 只处理文件（跳过目录）
    if os.path.isfile(filename):
        # 获取不带扩展名的文件名
        file_base = os.path.splitext(filename)[0]
        
        # 去除item_前缀（不区分大小写）
        # 先统一转小写判断前缀，再还原处理
        if file_base.lower().startswith("item_"):
            processed_name = file_base[5:]  # 去掉前5个字符（item_）
        else:
            processed_name = file_base
        
        # 转为大写
        upper_name = processed_name.upper()
        
        # 生成指定格式的代码行
        code_line = f"basicItem(mio_icif_resources.{upper_name}.get());"
        code_lines.append(code_line)

# 将所有行写入txt文件（UTF-8编码）
with open(output_file, "w", encoding="utf-8") as f:
    f.write("\n".join(code_lines))

# 输出提示信息
print(f"✅ 处理完成！")
print(f"📊 共生成 {len(code_lines)} 行代码")
print(f"📄 结果已保存至：{os.path.abspath(output_file)}")

# 等待用户输入，避免窗口直接关闭（可选）
input("按回车键退出...")