20171105T1530: It can be compiled with Solr 7.1.0.

IKAnalyzer：开源的基于java语言开发的轻量级的中文分词工具包。以开源项目Luence为应用主体的，结合词典分词和文法分析算法的中文分词组件。
新版本的 IKAnalyzer3.0则发展为面向Java的公用分词组件，独立于Lucene项目，同时提供了对Lucene的默认优化实现。 
IKAnalyzer的作者为林良益（linliangyi2007@gmail.com），项目网站为http://code.google.com/p/ik-analyzer/。 

===============================================================  
要安装到本地Maven repository，使用如下命令，将自动编译，打包并安装：
mvn clean install -Dmaven.test.skip=true 

============================= New ==================================  
Solr新版相关使用教程：http://blog.csdn.net/u010887744/article/category/6152594 【 http://csdn.zxiaofan.com 】

# <version>7.1.0</version>

现已更新升级，兼容solr6.6.0（ik-analyzer-solr6\target目录有ikanalyzer-6.6.0.jar，可直接使用）。   
By github.zxiaofan.com (2017:08:18 20:30)  

===============================================================

# <version>6.6.1</version>

支持IK动态分词以及禁用内置主词典main2012.dic；兼容旧版本（可不配置以下新节点）。
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">  
<properties>  
	<comment>IK Analyzer 扩展配置</comment>
	<!--用户可以在这里配置自己的扩展字典 -->
	<entry key="ext_dict">ext.dic;</entry> 
	<!--用户可以在这里配置自己的扩展停止词字典-->
	<entry key="ext_stopwords">stopword.dic;stop_1.dic</entry> 
	
	<!--词典动态更新时间间隔[首次延时,时间间隔]（格式：正整数，单位：分钟）-->
	<entry key="dic_updateMin">1,1</entry>
	
	<!--禁用内置主词典main2012.dic（默认false）-->
	<!--<entry key="dicInner_disable">true</entry> -->
</properties>

By github.zxiaofan.com (2017:10:13 20:00)
