import os

def generate_minecraft_items():
    # 获取当前脚本所在目录
    current_dir = os.getcwd()
    # 输出文件名
    output_file = "result.txt"
    # 获取脚本自身文件名，防止把脚本也写进去
    script_name = os.path.basename(__file__)
    
    results = []

    # 遍历当前目录
    for filename in os.listdir(current_dir):
        # 排除文件夹、脚本自身、以及生成的输出文件
        if os.path.isfile(filename) and filename != script_name and filename != output_file:
            # 获取不带后缀的文件名
            name_without_ext = os.path.splitext(filename)[0]
            
            # 处理变量名：去除 "item_" 前缀
            var_name = name_without_ext
            if var_name.startswith("item_"):
                var_name = var_name[5:]
            
            # 转换为全大写
            upper_var_name = var_name.upper()
            
            # 格式化字符串
            line = f'public static final DeferredItem<Item> {upper_var_name} = ITEMS.register("{name_without_ext}", () -> new Item(new Item.Properties()));'
            results.append(line)

    # 写入结果文件
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("\n".join(results))

    print(f"成功！已处理 {len(results)} 个文件，结果保存在 {output_file}")

if __name__ == "__main__":
    generate_minecraft_items()