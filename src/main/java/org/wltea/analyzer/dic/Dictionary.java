/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 * 
 * Update By github.zxiaofan.com [2017-10-11]
 * 
 */
package org.wltea.analyzer.dic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.wltea.analyzer.cfg.Configuration;

/**
 * 词典管理类,单子模式
 */
public class Dictionary {

    /*
     * 词典单子实例
     */
    private static Dictionary singleton;

    /*
     * 主词典对象
     */
    private static DictSegment _MainDict = null;

    /*
     * 停止词词典
     */
    private static DictSegment _StopWordDict = null;

    /*
     * 量词词典
     */
    private DictSegment _QuantifierDict;

    /**
     * 词典上传修改时间.
     */
    private static Map<String, Long> dicLastModified = new HashMap<String, Long>();

    /**
     * 扩展词.
     */
    private static Set<String> dicExtSet = new HashSet<String>(10000);

    /**
     * 停用词.
     */
    private static Set<String> dicStopSet = new HashSet<String>(2000);

    /**
     * 配置对象
     */
    private static Configuration cfg;

    /**
     * 线程池定时加载词典.
     */
    private static ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

    /**
     * 是否已加载过词典.
     */
    private static boolean hasAdd = false;

    /**
     * SimpleDateFormat（程序逻辑不存在并发，不考虑线程不安全情况）.
     */
    private final static java.text.SimpleDateFormat DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    /**
     * 词典初始化
     * 
     * 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
     * 
     * 只有当Dictionary类被实际调用时，才会开始载入词典， 这将延长首次分词操作的时间,
     * 
     * 该方法提供了一个在应用加载阶段就初始化字典的手段
     * 
     * @return Dictionary
     */
    public static Dictionary initial(Configuration cfg) {
        if (singleton == null) {
            synchronized (Dictionary.class) {
                if (singleton == null) {
                    singleton = new Dictionary(cfg);
                    Integer[] dicUpdateMin = cfg.getDicUpdateMin();
                    if (null != dicUpdateMin) {
                        print("loadDicFixedTime", "start");
                        loadDicFixedTime(dicUpdateMin);
                    }
                    return singleton;
                }
            }
        }
        return singleton;
    }

    /**
     * 定期加载配置文件.
     * 
     * @param dicUpdateMin
     *            加载间隔
     */
    private static void loadDicFixedTime(Integer[] dicUpdateMin) {
        scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                try {
                    loadMainDict();
                    loadStopWordDict();
                } catch (Exception e) {
                    print(e);
                }
            }
        }, dicUpdateMin[0], dicUpdateMin[1], TimeUnit.MINUTES);
    }

    private Dictionary(Configuration cfg) {
        this.cfg = cfg;
        this.loadMainDict();
        this.loadStopWordDict();
        this.loadQuantifierDict();
        hasAdd = true;
    }

    /**
     * 获取词典单子实例
     * 
     * @return Dictionary 单例对象
     */
    public static Dictionary getSingleton() {
        if (singleton == null) {
            throw new IllegalStateException("词典尚未初始化，请先调用initial方法");
        }
        return singleton;
    }

    /**
     * 批量加载新词条
     * 
     * @param words
     *            Collection<String>词条列表
     */
    public void addWords(Collection<String> words) {
        if (words != null) {
            for (String word : words) {
                if (word != null) {
                    // 批量加载词条到主内存词典中
                    singleton._MainDict.fillSegment(word.trim().toLowerCase().toCharArray());
                }
            }
        }
    }

    /**
     * 批量移除（屏蔽）词条
     * 
     * @param words
     */
    public void disableWords(Collection<String> words) {
        if (words != null) {
            for (String word : words) {
                if (word != null) {
                    // 批量屏蔽词条
                    singleton._MainDict.disableSegment(word.trim().toLowerCase().toCharArray());
                }
            }
        }
    }

    /**
     * 检索匹配主词典
     * 
     * @param charArray
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray) {
        return singleton._MainDict.match(charArray);
    }

    /**
     * 检索匹配主词典
     * 
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInMainDict(char[] charArray, int begin, int length) {
        return singleton._MainDict.match(charArray, begin, length);
    }

    /**
     * 检索匹配量词词典
     * 
     * @param charArray
     * @param begin
     * @param length
     * @return Hit 匹配结果描述
     */
    public Hit matchInQuantifierDict(char[] charArray, int begin, int length) {
        return singleton._QuantifierDict.match(charArray, begin, length);
    }

    /**
     * 从已匹配的Hit中直接取出DictSegment，继续向下匹配
     * 
     * @param charArray
     * @param currentIndex
     * @param matchedHit
     * @return Hit
     */
    public Hit matchWithHit(char[] charArray, int currentIndex, Hit matchedHit) {
        DictSegment ds = matchedHit.getMatchedDictSegment();
        return ds.match(charArray, currentIndex, 1, matchedHit);
    }

    /**
     * 判断是否是停止词
     * 
     * @param charArray
     * @param begin
     * @param length
     * @return boolean
     */
    public boolean isStopWord(char[] charArray, int begin, int length) {
        return singleton._StopWordDict.match(charArray, begin, length).isMatch();
    }

    /**
     * 加载主词典及扩展词典
     */
    private static void loadMainDict() {
        // 建立一个主词典实例
        if (_MainDict == null) { // 首次加载
            _MainDict = new DictSegment((char) 0);
            String mainDictionary = cfg.getMainDictionary();
            // 读取主词典文件
            if (!cfg.isDicDisable()) {
                loadToMain(mainDictionary, 1);
            }
        }
        // 加载扩展词典
        List<String> extDictFiles = cfg.getExtDictionarys();
        if (null != extDictFiles && !extDictFiles.isEmpty()) {
            for (String extFile : extDictFiles) {
                loadToMain(extFile, null);
            }
        }
    }

    /**
     * 将文件加载到主库.
     * 
     * @param mainDictionary
     *            mainDictionary
     * @param innerDic
     *            是否是内置词典（1是）
     */
    private static void loadToMain(String mainDictionary, Integer innerDic) {

        String path = null;
        InputStream is = null;
        File file = new File("");
        if (Objects.equals(1, innerDic)) {
            is = Dictionary.class.getClassLoader().getResourceAsStream(mainDictionary);
        } else {
            path = getFilePath(mainDictionary);
            file = new File(path);
            try {
                is = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                print(e);
            }
        }
        if (is == null) {
            print("loadToMain:FileNotFoundException", path);
            // throw new RuntimeException("Main Dictionary not found!!!");
            return;
        }
        if (hasAdd && dicLastModified.containsKey(path) && file.lastModified() <= dicLastModified.get(path)) {
            return; // 非首次加载或词典未修改
        }
        print("loadToMain_START", mainDictionary);
        BufferedReader br = null;
        InputStreamReader inputStreamReader = null;
        StringBuilder updateDic = new StringBuilder();
        try {
            inputStreamReader = new InputStreamReader(is, "UTF-8");
            br = new BufferedReader(inputStreamReader, 512);
            String theWord = null;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    if (!dicExtSet.contains(theWord)) {
                        dicExtSet.add(theWord);
                        if (hasAdd) {
                            updateDic.append(theWord).append(";");
                        }
                    }
                    _MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            } while (theWord != null);

        } catch (IOException ioe) {
            print("loadToMain exception.");
            print(ioe);
        } finally {
            dicLastModified.put(path, file.lastModified());
            if (updateDic.length() != 0) {
                print("loadToMain_END", "FileLastModified:" + DATE_FORMAT.format(new Date(file.lastModified())), updateDic.toString());
            }
            close(is, inputStreamReader, br);
        }
    }

    /**
     * 获取字典文件实际路径.
     * 
     * @param dictionary
     *            字典名
     * @return 字典路径
     */
    private static String getFilePath(String dictionary) {
        URL resource = Dictionary.class.getClassLoader().getResource(dictionary);
        if (null == resource) {
            print("NullPointerException", "getFilePath", dictionary); // 提示用户配置词库有误，方便用户定位异常
        }
        return resource.getPath(); // 抛出异常，终止IK
    }

    /**
     * 加载用户配置的扩展词典到主词库表【此方法弃用】
     */
    @SuppressWarnings("unused")
    private void loadExtDict() {
        // 加载扩展词典配置
        List<String> extDictFiles = cfg.getExtDictionarys();
        if (extDictFiles != null) {
            InputStream is = null;
            for (String extDictName : extDictFiles) {
                // 读取扩展词典文件
                print("loadExtDict_START", extDictName);
                is = this.getClass().getClassLoader().getResourceAsStream(extDictName);
                // 如果找不到扩展的字典，则忽略
                if (is == null) {
                    continue;
                }
                BufferedReader br = null;
                InputStreamReader inputStreamReader = null;
                try {
                    inputStreamReader = new InputStreamReader(is, "UTF-8");
                    br = new BufferedReader(inputStreamReader, 512);
                    String theWord = null;
                    do {
                        theWord = br.readLine();
                        if (theWord != null && !"".equals(theWord.trim())) {
                            // 加载扩展词典数据到主内存词典中
                            // System.out.println(theWord);
                            _MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                        }
                    } while (theWord != null);

                } catch (IOException ioe) {
                    print("loadExtDict exception.");
                    print(ioe);
                } finally {
                    close(is, inputStreamReader, br);
                }
            }
        }
    }

    /**
     * 加载用户扩展的停止词词典
     */
    private static void loadStopWordDict() {
        // 建立一个主词典实例
        if (_StopWordDict == null) {
            _StopWordDict = new DictSegment((char) 0);
        }
        // 加载扩展停止词典
        List<String> extStopWordDictFiles = cfg.getExtStopWordDictionarys();
        if (extStopWordDictFiles != null) {
            InputStream is = null;
            for (String extStopWordDictName : extStopWordDictFiles) {
                // 读取扩展词典文件
                // is = Dictionary.class.getClassLoader().getResourceAsStream(extStopWordDictName);
                String path = getFilePath(extStopWordDictName);
                File file = new File(path);
                try {
                    is = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    print("loadStopWordDict:FileNotFoundException", path);
                    print(e);
                } finally {
                    close(is);
                }
                // 如果找不到扩展的字典，则忽略
                if (is == null) {
                    continue;
                }
                if (hasAdd && dicLastModified.containsKey(path) && file.lastModified() <= dicLastModified.get(path)) {
                    continue; // 非首次加载或词典未修改
                }
                print("loadStopWordDict_START", extStopWordDictName);
                BufferedReader br = null;
                InputStreamReader inputStreamReader = null;
                StringBuilder updateDic = new StringBuilder();
                try {
                    inputStreamReader = new InputStreamReader(is, "UTF-8");
                    br = new BufferedReader(inputStreamReader, 512);
                    String theWord = null;
                    do {
                        theWord = br.readLine();
                        if (theWord != null && !"".equals(theWord.trim())) {
                            // System.out.println(theWord);
                            // 加载扩展停止词典数据到内存中
                            _StopWordDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                            if (!dicStopSet.contains(theWord)) {
                                dicStopSet.add(theWord);
                                if (hasAdd) {
                                    updateDic.append(theWord).append(";");
                                }
                            }
                        }
                    } while (theWord != null);

                } catch (IOException ioe) {
                    print("loadStopWordDict exception.");
                    print(ioe);
                } finally {
                    dicLastModified.put(path, file.lastModified());
                    if (updateDic.length() != 0) {
                        print("loadStopWordDict_END", "FileLastModified:" + DATE_FORMAT.format(new Date(file.lastModified())), updateDic.toString());
                    }
                    close(is, inputStreamReader, br);
                }
            }
        }
    }

    /**
     * 加载量词词典
     */
    private void loadQuantifierDict() {
        // 建立一个量词典实例
        _QuantifierDict = new DictSegment((char) 0);
        // 读取量词词典文件
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getQuantifierDicionary());
        if (is == null) {
            throw new RuntimeException("Quantifier Dictionary not found!!!");
        }
        BufferedReader br = null;
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(is, "UTF-8");
            br = new BufferedReader(inputStreamReader, 512);
            String theWord = null;
            do {
                theWord = br.readLine();
                if (theWord != null && !"".equals(theWord.trim())) {
                    _QuantifierDict.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            } while (theWord != null);

        } catch (IOException ioe) {
            print("Quantifier Dictionary loading exception.");
            print(ioe);

        } finally {
            close(is, inputStreamReader, br);
        }
    }

    /**
     * 批量关闭文件流.
     * 
     * @param closeables
     *            文件流集合
     */
    private static void close(AutoCloseable... closeables) {
        if (null != closeables && closeables.length > 0) {
            for (AutoCloseable autoCloseable : closeables) {
                if (null != autoCloseable) {
                    try {
                        autoCloseable.close();
                    } catch (Exception e) {
                        print(e);
                    }
                }
            }
        }
    }

    /**
     * 控制台打印.
     * 
     * @param param
     *            参数
     */
    public static void print(String... param) {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(DATE_FORMAT.format(new Date())).append("]");
        for (String str : param) {
            builder.append("[").append(str).append("]");
        }
        System.out.println(builder.toString());
    }

    /**
     * 控制台打印.
     * 
     * @param e
     *            异常信息
     */
    public static void print(Exception e) {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(DATE_FORMAT.format(new Date())).append("]").append(e.getMessage());
        System.out.println(builder.toString());
        e.printStackTrace();
    }
}
