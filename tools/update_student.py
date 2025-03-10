from tools import post_image_to_remote, replace0, update_image_from_api
from fetch_student_info_from_ba_game_db import test_name_exist, download_image, query_remote_name
import os
import cv2
import numpy as np
base_folder = "/student_rank/"

# 更新学生
# tencentcloud-sdk-python-common==3.0.754

if __name__ == '__main__':
    # 将夜喵的图和wiki图整合
    # file_list = os.listdir("./image" + base_folder)
    # for file in file_list:
    #     file_name = file.replace(".png", "")
    #     file_path = file_path = "./image" + base_folder + file
    #     file_names = list(map(lambda n: replace0(n), file_name.split("_")))
    #     stu_name = file_names[0]
    #     new_student = False
    #     # 第一个名字不存在
    #     if not test_name_exist(file_names[0]):
    #         # 且没有第二个名字或者第二个名字也不存在, 说明是新学生, 不做操作
    #         if len(file_names) > 1:
    #             if not test_name_exist(file_names[1]):
    #                 new_student = True
    #             else:
    #                 stu_name = file_names[1]
    #         else:
    #             new_student = True
    #     if new_student:
    #         print("new student: %s" % stu_name)
    #     else:
    #         print("process old student: %s" % stu_name)
    #         # 下载远端图片
    #         remote = query_remote_name(stu_name)
    #         if not remote:
    #             print("error at: %s" % stu_name)
    #             exit(-1)
    #         path = str(remote["path"])
    #         png_name = path.replace("/student_rank/", "")
    #         local_path = "./image/parse/%s" % png_name
    #         source_im = download_image("https://arona.cdn.diyigemt.com/image", path, local_path)
    #         replace_im = cv2.imdecode(np.fromfile(file_path, dtype=np.uint8), -1)
    #         rows, cols, _ = replace_im.shape
    #         source_im[0:rows,0:cols] = replace_im
    #         cv2.imencode(".png", source_im)[1].tofile(file_path)
    #         os.remove(local_path)
    #         print("student: %s process success" % stu_name)
    update_image_from_api(base_folder, type=1)
    post_image_to_remote(base_folder)
