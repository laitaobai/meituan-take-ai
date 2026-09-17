package com.sky.taobai.config;

import com.sky.taobai.exceptian.CommonException;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DishVectorStore {
    @Autowired
    public SimpleVectorStore simpleVectorStore;

       @PostConstruct
    public void init(){
       String text = """
                #简单添加菜品数据,
                #以下是参考资料
                 这个店名是"不好吃且贵"
                 你是这个店里的点菜ai助手
                 这是一份店里的菜单 :
                1|饮料名字：王老吉         价格: 6.00
                2|饮料名字：北冰洋          价格:    4.00
                3|饮料名字：雪花啤酒        价格:    4.00
                1|菜品名字：米饭           价格:   9999.00
                2|菜品名字：馒头            价格:    1.00
                3|菜品名字：老坛酸菜鱼       价格:   56.00
                4|菜品名字：经典酸菜鮰鱼     价格:    66.00
                5|菜品名字：蜀味水煮草鱼      价格:   38.00
                6|菜品名字：清炒小油菜       价格:   18.00
                7|菜品名字：蒜蓉娃娃菜       价格:   18.00
                8|菜品名字：清炒西兰花       价格:   18.00
                 #以下是推荐菜品，和菜品搭配
                 1.老坛酸菜鱼  搭配菜品:清炒小油菜
                 2.清炒西兰花  搭配菜品:雪花啤酒
             """;
        Document document = new Document(text);
        if (document != null && !document.getText().equals("")){
            try{
                simpleVectorStore.add(List.of(document));
                System.out.println("添加成功");
            } catch (Exception e) {
                throw new CommonException("添加失败，向量数据库为空");
            }
        }else {
            System.out.println("方法未执行，文档转换失败，向量数据库为空");
        }



    }
}
