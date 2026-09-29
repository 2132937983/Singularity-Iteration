import os

# 获取当前脚本文件名（用于排除自身）
current_script = os.path.basename(__file__)
# 定义输出的txt文件名（区分之前的items文件）
output_file = "blocks_code.txt"
# 存储生成的代码行
code_lines = []

# 遍历当前目录下的所有文件
for filename in os.listdir("."):
    # 排除当前脚本和输出的txt文件，避免处理无关文件
    if filename == current_script or filename == output_file:
        continue
    
    # 仅处理文件（跳过目录）
    if os.path.isfile(filename):
        # 提取不带扩展名的文件名（核心处理对象）
        file_base = os.path.splitext(filename)[0]
        
        # 去除item_前缀（不区分大小写，仅匹配开头的item_）
        if file_base.lower().startswith("item_"):
            processed_name = file_base[5:]  # 移除前5个字符（item_）
        else:
            processed_name = file_base
        
        # 转为大写（保留下划线、数字等字符）
        upper_name = processed_name.upper()
        
        # 生成新的指定格式语句
        code_line = f"output.accept(mio_icif_items.{upper_name}.get());"
        code_lines.append(code_line)

# 将所有行写入txt文件（UTF-8编码，避免中文乱码）
with open(output_file, "w", encoding="utf-8") as f:
    f.write("\n".join(code_lines))

# 输出清晰的完成提示
print(f"✅ 处理完成！")
print(f"📊 共生成 {len(code_lines)} 行代码")
print(f"📄 结果已保存至：{os.path.abspath(output_file)}")

# 等待用户输入，避免运行后窗口直接关闭
input("按回车键退出...")