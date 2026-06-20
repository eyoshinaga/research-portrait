#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
科研人员画像分析系统 - 测试数据生成器
校内系统: 学院/系/研究所/研究中心/重点实验室/教学部 + 500研究人员 / 500论文 / 300专利 / 300项目
输出: test-data.sql (MySQL) + neo4j-test-data.cypher (Neo4j)
"""
import random
import datetime

random.seed(42)  # 可复现

# ============================================================
# 数据池
# ============================================================

# 100+ 姓氏
SURNAMES = list('陈林周杨王李张赵刘孙吴郑黄何罗梁宋韩唐冯邓曹彭董袁谢邹田蔡杜魏任姚卢贾苏蒋范沈薛严段钟汪熊石侯朱秦程许傅曾谭夏胡吕丁高郭马罗梁宋韩唐冯邓曹彭董袁谢邹田蔡杜魏任姚卢贾苏蒋范沈薛严段钟汪熊石侯朱秦程许傅曾谭夏胡吕丁高郭马廖虞万方石金薛叶贺姚方汤尹邹')

# 男性名字 (双字)
MALE_NAMES = [
    '志远','明辉','海波','建国','国华','文博','军强','志强','伟明','浩然',
    '文涛','振邦','海涛','明远','志刚','建军','鹏飞','振华','伟峰','文东',
    '国栋','建华','志辉','晓东','明远','海涛','俊豪','子轩','浩宇','嘉铭',
    '天宇','奕辰','宇轩','子墨','彦霖','思远','博文','晨阳','旭东','锦程',
    '凯文','昊然','睿杰','智渊','哲翰','泽宇','俊熙','骏杰','承恩','修远',
    '致远','景行','崇文','启明','立伟','宏达','学军','德昌','安民','永康',
]

# 女性名字 (双字)
FEMALE_NAMES = [
    '婉清','雪梅','婷婷','晓燕','美玲','静怡','冰清','丽华','春华','佳琪',
    '红梅','秀兰','晓琳','晓峰','丽萍','玉芬','红梅','美华','佳慧','晓琳',
    '梦瑶','若曦','诗涵','欣怡','思琪','雨桐','婧琪','雅雯','若彤','芷晴',
    '梦琪','语嫣','婉如','清韵','慧敏','敏华','慧芳','淑芬','丽娟','美珍',
    '晓雯','雪琴','凤英','桂芳','玉兰','秀英','素珍','月华','凤霞','丽君',
]

# ============================================================
# 校内部门数据池
# ============================================================

# 学院 (综合性教学科研单位)
COLLEGES = [
    # 工学类学院
    '计算机科学与技术学院','软件学院','电子信息与工程学院','自动化学院','信息与通信工程学院',
    '机械工程学院','材料科学与工程学院','土木工程学院','化学化工学院','能源与动力工程学院',
    '电气工程学院','航空航天学院','生物医学工程学院','海洋工程学院','车辆工程学院',
    '安全科学与工程学院','测绘科学与技术学院','交通工程学院','食品科学与工程学院','纺织科学与工程学院',
    # 理学类学院
    '数学科学学院','物理学院','化学学院','生命科学学院','天文与空间科学学院',
    '大气科学学院','地球科学学院','统计学院','地理科学学院',
    # 医学类学院
    '基础医学院','临床医学院','药学院','公共卫生学院','口腔医学院','护理学院','中医学院',
    # 管理类学院
    '经济管理学院','商学院','公共管理学院','会计学院','旅游与城市管理学院',
    # 人文社科类学院
    '文学院','历史学院','法学院','外国语学院','新闻与传播学院','教育学院',
    '社会学院','马克思主义学院','艺术学院','国际关系学院',
]

# 系 (按学科划分的教学科研单位)
DEPARTMENTS = [
    '计算机科学与技术系','电子工程系','自动化系','机械工程系','土木工程系',
    '数学系','物理系','化学系','生物系','力学系',
    '材料科学与工程系','能源与动力工程系','电气工程系','化学工程系','测控技术与仪器系',
    '信息安全系','通信工程系','光电信息工程系','建筑学系','工程力学系',
]

# 研究所 (专门研究机构)
RESEARCH_INSTITUTES = [
    '人工智能研究所','软件工程研究所','网络安全研究所','集成电路研究所','量子信息研究所',
    '生物医学工程研究所','纳米技术研究所','能源科学研究所','环境科学研究所','精密仪器研究所',
    '材料科学研究所','航空航天研究所','智能制造研究所','海洋技术研究所','核科学研究所',
    '生物技术研究所','药物研究所','人口研究所','经济研究所','高等教育研究所',
]

# 研究中心 (跨学科研究平台)
RESEARCH_CENTERS = [
    '智能计算研究中心','大数据研究中心','机器人研究中心','新材料研究中心','航空航天研究中心',
    '生命科学研究中心','脑科学研究中心','量子计算研究中心','智能制造研究中心','新能源研究中心',
    '智慧交通研究中心','数字医疗研究中心','环境工程研究中心','生物信息学研究中心','光电技术研究中心',
    '区块链研究中心','认知科学研究中心','城市治理研究中心','文化创意产业研究中心','公共政策研究中心',
]

# 重点实验室 (国家级/省部级重点实验室)
KEY_LABS = [
    '人工智能重点实验室','计算机网络与信息安全重点实验室','软件工程重点实验室',
    '智能控制重点实验室','精密制造重点实验室','生物医学重点实验室',
    '纳米材料重点实验室','量子信息重点实验室','高温超导重点实验室','智能感知重点实验室',
    '智能制造重点实验室','基因编辑重点实验室','新能源材料重点实验室','空间科学重点实验室',
]

# 教学部 (公共课教学单位)
TEACHING_DEPTS = [
    '思想政治理论课教学部','数学教学部','物理教学部','化学教学部',
    '外语教学部','体育教学部','计算机基础教学部','工程训练中心',
]

# 研究室/课题组 (科研人员所属的具体研究单元)
RESEARCH_GROUPS = {
    '工学': [
        '人工智能实验室','机器学习研究组','自然语言处理实验室','计算机视觉研究组',
        '网络安全实验室','软件工程研究室','体系结构实验室','物联网研究组',
        '机器人实验室','智能制造研究组','精密加工实验室','材料表征研究室',
        '微电子实验室','光通信研究组','信号处理实验室','通信网络研究组',
        '电力系统实验室','新能源研究组','飞行器设计实验室','控制工程研究室',
        '结构工程实验室','岩土工程研究组','化工工艺实验室','催化工程研究室',
        '车辆工程实验室','交通工程研究组','生物医学工程实验室','海洋技术研究组',
    ],
    '理学': [
        '量子计算实验室','凝聚态物理研究组','高能物理实验室','光学实验室',
        '纳米材料研究室','配位化学研究组','有机合成实验室','分析化学研究室',
        '基因组学实验室','蛋白质研究组','细胞生物学实验室','神经科学研究室',
        '微生物实验室','免疫学研究组','统计建模研究室','运筹学实验室',
        '数论研究组','代数几何实验室','拓扑学研究室','流体力学实验室',
    ],
    '医学': [
        '肿瘤研究实验室','心血管研究组','神经科学实验室','药物设计研究室',
        '干细胞实验室','免疫治疗研究组','医学影像实验室','临床药学研究室',
        '口腔医学实验室','流行病学研究组','护理学研究室','中药药理实验室',
    ],
    '管理学': [
        '供应链研究实验室','创新管理研究组','金融工程实验室','电商研究组',
        '组织行为研究室','战略管理研究组','项目管理实验室','评估研究组',
    ],
    '人文社科': [
        '科技哲学研究室','语言认知实验室','教育技术研究组','法律政策研究室',
        '传播学实验室','社会调查研究中心','科技史研究室','心理学实验室',
    ],
}

# 职称
TITLES = ['教授','副教授','讲师','助理教授','副研究员','研究员','讲师','副教授','教授','特聘研究员']

# 研究方向 (按学科门类)
RESEARCH_FIELDS = {
    '工学': [
        '人工智能与机器学习','自然语言处理与语义理解','计算机视觉与深度学习','知识图谱与自然语言理解',
        '大数据处理与分布式系统','网络安全与区块链技术','软件工程与形式化验证','计算机体系结构与芯片设计',
        '强化学习与智能决策','边缘计算与物联网安全','数字孪生与智能制造','工业机器人与协作控制',
        '集成电路设计与测试','微纳制造与精密加工','光电子学与光通信','无线通信与信号处理',
        '太赫兹通信技术','卫星导航与定位技术','无人机集群与协同控制','电力电子与新能源系统',
        '智能制造与精密加工','飞行器设计与控制','航空航天嵌入式系统','结构工程与抗震设计',
        '纳米材料与储能器件','柔性电子与功能材料','含能材料与推进技术','传感器技术与微系统',
        '水处理与膜分离技术','环境污染治理与生态修复','遥感技术与地理信息系统','水资源优化配置',
        '交通信号优化与控制','建筑能耗优化系统','区块链与分布式安全','机器学习与数据挖掘',
    ],
    '理学': [
        '量子信息与量子计算','凝聚态物理与超导材料','星系形成与宇宙学','高能物理与粒子物理',
        '原子分子与光物理','光学与激光技术','密码学与信息安全','运筹学与优化理论',
        '有机合成与药物化学','配位化学与金属有机框架','无机化学与材料化学','分析化学与传感技术',
        '基因组学与生物信息学','植物分子生物学','动物行为与保护生物学','微生物学与免疫学',
        '蛋白质结构与功能','细胞生物学与发育生物学','神经科学与脑科学','生物物理与生物化学',
        '气候变化与极端天气','大气化学与环境','地球物理与空间物理','古生物学与进化生物学',
        '统计学与数据科学','代数几何与数论','拓扑学与几何学','流体力学与计算力学',
    ],
    '医学': [
        '肿瘤精准治疗与免疫疗法','心血管疾病基础与临床','神经科学与脑科学','药物设计与分子模拟',
        '口腔医学与生物材料','医学影像分析','干细胞与再生医学','免疫学与自身免疫疾病',
        '感染性疾病与疫苗开发','代谢性疾病研究','遗传病基因治疗','生物医学工程',
    ],
    '管理学': [
        '供应链优化与决策分析','创新管理与科技政策','金融工程与风险管理','信息系统与电子商务',
        '组织行为与人力资源','战略管理与企业理论','运筹与管理科学','项目管理与评估',
    ],
    '人文社科': [
        '科技哲学与伦理','语言认知与计算','教育技术与学习科学','法律与科技政策',
        '传播学与新媒体','社会学与公共政策','科技史与科技文化','心理与认知科学',
    ],
}

# 期刊名
JOURNALS = [
    '计算机学报','软件学报','自动化学报','计算机研究与发展','中文信息学报','密码学报',
    '信息安全学报','模式识别与人工智能','数据分析与知识发现','电子学报','通信学报',
    '机器人','航空学报','宇航学报','机械工程学报','中国电机工程学报','系统工程理论与实践',
    '控制与决策','系统科学与数学','管理科学学报','物理学报','中国科学：物理学',
    '低温物理学报','物理化学学报','无机材料学报','材料科学与工程学报','金属学报',
    '复合材料学报','纳米研究','化学学报','有机化学','无机化学学报','分析化学',
    '生物化学与生物物理进展','中国生物医学工程学报','中国医学科学院学报','生物物理学报',
    '微生物学报','遗传','药学学报','中华心血管病杂志','中国神经科学杂志','细胞生物学杂志',
    '环境科学','大气科学','遥感学报','水利学报','计算机集成制造系统','传感器与微系统',
    '导航定位与授时','中国激光','光学学报','半导体学报','电力系统自动化','电网技术',
    '中华医学杂志','中华外科杂志','中国药学杂志','中国药理学通报','实验生物学报',
    '地理学报','地球物理学报','气象学报','海洋学报','生态学报','应用生态学报',
    '测绘学报','武汉大学学报(信息科学版)','交通运输工程学报','土木工程学报','建筑结构学报',
    '心理科学进展','教育研究','社会学研究','法学研究','新闻与传播研究','管理世界',
    '科研管理','科学学研究','中国软科学','系统工程','运筹学学报','中国管理科学',
    '计算机科学','电子学报','通信学报','信号处理','电路与系统学报','微电子学',
    '真空科学与技术学报','材料导报','功能材料','硅酸盐学报','人工晶体学报',
    '催化学报','分子催化','色谱','质谱学报','核技术','原子核物理',
]

# 学科领域
SUBJECT_FIELDS = [
    '计算机科学','软件工程','人工智能','数据挖掘','信息安全','机器人技术',
    '量子物理','凝聚态物理','超导物理','计算化学','材料科学','复合材料',
    '纳米技术','分子生物学','生物医学','肿瘤学','生物物理','微生物学',
    '遗传学','药学','心血管病学','神经科学','细胞生物学','机械工程',
    '制造技术','航空航天','电子通信','航天技术','导航技术','电力工程',
    '通信技术','传感器技术','运筹学','系统科学','控制理论','管理科学',
    '环境科学','气象学','遥感技术','水利工程','光学工程','半导体物理',
    '电力系统','医学影像','免疫学','药物化学','生态学','地理信息',
    '交通运输','土木工程','建筑科学','心理学','教育学','社会学',
    '法学','传播学','科技管理','化学工程','能源科学','核科学',
]

# 专利类型
PATENT_TYPES = ['发明专利','发明专利','发明专利','发明专利','实用新型']

# 专利状态
PATENT_STATUSES = ['授权','授权','授权','实审']

# 项目来源
PROJECT_SOURCES = [
    '国家自然科学基金委','国家自然科学基金委','国家自然科学基金委','科技部','科技部',
    '工信部','教育部','中科院','国防科工局','国家卫健委','生态环境部','自然资源部',
    '水利部','农业农村部','省科技厅','省科技厅','省住建厅','省教育厅','企业横向','网信办',
]

# 项目级别
PROJECT_LEVELS = ['国家级','国家级','国家级','省部级','省部级','横向']

# 项目状态
PROJECT_STATUSES = ['在研','在研','在研','结题']

# 国家 (校内系统，统一为中国)
COUNTRIES = ['中国']

# ============================================================
# 生成函数
# ============================================================

def generate_name():
    """生成真实感的中文名字"""
    surname = random.choice(SURNAMES)
    if random.random() < 0.55:
        given = random.choice(MALE_NAMES)
    else:
        given = random.choice(FEMALE_NAMES)
    return surname + given

def generate_institutions():
    """生成校内部门（学院/系/研究所/研究中心/重点实验室/教学部）"""
    insts = []
    # 学院 -> type='学院'
    for name in COLLEGES:
        insts.append((name, '学院', '中国'))
    # 系 -> type='系'
    for name in DEPARTMENTS:
        insts.append((name, '系', '中国'))
    # 研究所 -> type='研究所'
    for name in RESEARCH_INSTITUTES:
        insts.append((name, '研究所', '中国'))
    # 研究中心 -> type='研究中心'
    for name in RESEARCH_CENTERS:
        insts.append((name, '研究中心', '中国'))
    # 重点实验室 -> type='重点实验室'
    for name in KEY_LABS:
        insts.append((name, '重点实验室', '中国'))
    # 教学部 -> type='教学部'
    for name in TEACHING_DEPTS:
        insts.append((name, '教学部', '中国'))
    
    return insts

def generate_researchers(institutions):
    """生成500个科研人员"""
    researchers = []
    used_names = set()
    
    for i in range(500):
        # 生成不重复的名字
        name = generate_name()
        attempts = 0
        while name in used_names and attempts < 10:
            name = generate_name()
            attempts += 1
        used_names.add(name)
        
        age = random.randint(28, 58)
        
        # 随机学科门类 (工学占多数)
        discipline = random.choices(
            ['工学','理学','医学','管理学','人文社科'],
            weights=[50, 25, 12, 8, 5]
        )[0]
        
        department = random.choice(RESEARCH_GROUPS[discipline])
        title = random.choice(TITLES)
        field = random.choice(RESEARCH_FIELDS[discipline])
        
        # 年龄越大职称越高
        if age >= 50:
            title = random.choice(['教授','教授','研究员','特聘研究员','副教授'])
        elif age >= 40:
            title = random.choice(['教授','副教授','副教授','研究员'])
        elif age >= 35:
            title = random.choice(['副教授','副教授','讲师','助理教授'])
        else:
            title = random.choice(['讲师','讲师','助理教授','副研究员'])
        
        # 影响力指数: 职称和年龄相关
        base = {'教授':70,'研究员':72,'特聘研究员':78,'副教授':55,'副研究员':50,
                '助理教授':45,'讲师':35}.get(title, 40)
        influence = round(base + random.uniform(-15, 25), 2)
        influence = max(10.0, min(99.9, influence))
        
        institution_id = random.randint(1, len(institutions))
        
        researchers.append({
            'name': name, 'age': age, 'department': department,
            'title': title, 'field': field, 'discipline': discipline,
            'institution_id': institution_id, 'influence': influence
        })
    
    return researchers

def generate_papers():
    """生成500篇论文"""
    papers = []
    title_templates = [
        '基于{method}的{field}方法研究',
        '面向{field}的{method}架构设计与优化',
        '{field}中的{method}关键技术研究',
        '{method}在{field}中的应用研究',
        '一种{method}驱动的{field}新方法',
        '基于{method}的{field}系统设计与实现',
        '{field}领域{method}研究进展与展望',
        '面向{field}的{method}模型与算法',
        '基于{method}的{field}性能优化研究',
        '{field}的{method}机制分析与验证',
        '融合{method}的{field}综合方法',
        '{method}支持下的{field}创新研究',
        '{field}中{method}的理论框架与实践',
        '面向{field}的{method}综述',
        '{method}在{field}中的最新进展',
    ]
    methods = [
        '深度学习','强化学习','图神经网络','联邦学习','知识蒸馏','迁移学习',
        'Transformer架构','注意力机制','对比学习','生成对抗网络','自监督学习',
        '多模态融合','边缘计算','区块链技术','数字孪生','量子计算',
        '密度泛函理论','分子动力学模拟','CRISPR基因编辑','单细胞测序',
        '有限元分析','蒙特卡洛方法','贝叶斯优化','博弈论','拓扑优化',
        '大数据分析','知识图谱','自然语言处理','计算机视觉','强化学习',
    ]
    
    used_titles = set()
    for i in range(500):
        template = random.choice(title_templates)
        method = random.choice(methods)
        field = random.choice(SUBJECT_FIELDS)
        title = template.format(method=method, field=field)
        
        # 确保不重复
        attempts = 0
        while title in used_titles and attempts < 5:
            template = random.choice(title_templates)
            method = random.choice(methods)
            field = random.choice(SUBJECT_FIELDS)
            title = template.format(method=method, field=field)
            attempts += 1
        used_titles.add(title)
        
        journal = random.choice(JOURNALS)
        year = random.randint(2019, 2025)
        month = random.randint(1, 12)
        day = random.randint(1, 28)
        pub_date = f'{year}-{month:02d}-{day:02d}'
        
        # 引用次数与发表年份相关
        years_ago = 2025 - year
        cited = int(random.expovariate(1/50) * (1 + years_ago * 0.5))
        cited = min(cited, 500)
        
        doi = f'10.1000/{journal[:2].lower()}-{year}-{i+1:04d}'
        impact = round(random.uniform(1.5, 5.0), 1)
        subject = field
        
        papers.append({
            'title': title, 'journal': journal, 'doi': doi,
            'pub_date': pub_date, 'cited': cited, 'subject': subject, 'impact': impact
        })
    
    return papers

def generate_patents():
    """生成300项专利"""
    patents = []
    title_templates = [
        '一种基于{method}的{field}系统及方法',
        '面向{field}的{method}装置',
        '{method}驱动的{field}设备及其控制方法',
        '一种{field}的{method}方法及系统',
        '{field}用{method}装置及制备方法',
        '基于{method}的{field}检测系统',
        '一种{field}{method}装置及工艺',
        '{method}在{field}中的应用装置',
        '面向{field}的{method}系统及控制方法',
        '一种{method}的{field}实现方法及装置',
    ]
    methods = [
        '深度学习','智能感知','自适应控制','多模态融合','边缘计算','区块链',
        '量子加密','纳米结构','柔性传感','生物识别','数字孪生','知识图谱',
        '联邦学习','图神经网络','强化学习','注意力机制','光通信','太赫兹',
        'MEMS微纳','超导磁体','CRISPR','药物递送','脑机接口','卫星导航',
    ]
    fields = [
        '目标检测','任务调度','健康监测','安全增强','波束成形','推荐系统',
        '太阳能电池','压力传感','基因编辑','药物靶向','信号解码','视觉引导',
        '抗干扰接收','拓扑控制','磁体绕制','电子皮肤','影像分析','膜分离',
        '能耗优化','数字孪生平台','含能材料合成','生物特征识别','智能超表面','协作机器人',
    ]
    
    used_names = set()
    for i in range(300):
        template = random.choice(title_templates)
        method = random.choice(methods)
        field = random.choice(fields)
        name = template.format(method=method, field=field)
        
        attempts = 0
        while name in used_names and attempts < 5:
            template = random.choice(title_templates)
            method = random.choice(methods)
            field = random.choice(fields)
            name = template.format(method=method, field=field)
            attempts += 1
        used_names.add(name)
        
        patent_no = f'ZL{random.randint(2019,2024)}{random.randint(10000,99999):05d}'
        apply_year = random.randint(2019, 2024)
        apply_month = random.randint(1, 12)
        apply_day = random.randint(1, 28)
        apply_date = f'{apply_year}-{apply_month:02d}-{apply_day:02d}'
        
        patent_type = random.choice(PATENT_TYPES)
        status = random.choice(PATENT_STATUSES)
        
        grant_date = None
        if status == '授权':
            grant_year = apply_year + random.randint(1, 3)
            grant_month = random.randint(1, 12)
            grant_day = random.randint(1, 28)
            if grant_year <= 2025:
                grant_date = f'{grant_year}-{grant_month:02d}-{grant_day:02d}'
        
        patents.append({
            'name': name, 'patent_no': patent_no, 'apply_date': apply_date,
            'grant_date': grant_date, 'patent_type': patent_type, 'status': status
        })
    
    return patents

def generate_projects():
    """生成300个项目"""
    projects = []
    name_templates = [
        '{field}关键技术研究',
        '面向{field}的基础研究与应用',
        '{field}核心算法与平台研发',
        '{field}机理与调控机制研究',
        '基于{method}的{field}研究',
        '{field}中{method}的理论与方法',
        '{field}创新平台建设',
        '{field}综合评估与决策支持',
        '{field}智能系统研发',
        '{field}前沿技术攻关',
    ]
    fields_list = [
        '新一代人工智能','工业互联网安全','量子计算','大规模知识图谱',
        '太赫兹通信','智能制造数字孪生','纳米材料柔性传感器','高温超导材料',
        'CRISPR基因编辑治疗','脑科学与类脑研究','深度学习安全鲁棒性',
        '工业机器人协作控制','联邦学习隐私保护','航空航天嵌入式系统',
        '精准医学大数据','超大规模天线阵列','新能源材料计算',
        '智能交通信号优化','碳纳米管复合材料','蛋白质结构预测',
        '水资源优化配置','气候变化影响评估','遥感数据智能处理',
        '供应链协同优化','区块链身份认证','柔性电子多模态感知',
        '环境污染治理','含能材料合成','大语言模型安全评估',
        '微纳卫星编队飞行','生物信息学','光电子芯片','固态电池',
        '核聚变材料','太空碎片清理','深海探测装备','碳中和路径',
        '智慧农业平台','脑机接口','类脑芯片','量子通信网络',
        '生物制造','合成生物学','可解释AI','数字孪生城市',
        '自动驾驶感知','6G通信','基因治疗','纳米药物递送',
    ]
    methods = ['深度学习','强化学习','图神经网络','联邦学习','多模态融合',
               '数字孪生','量子计算','大数据分析','区块链','知识图谱']
    
    used_names = set()
    for i in range(300):
        template = random.choice(name_templates)
        field = random.choice(fields_list)
        method = random.choice(methods)
        name = template.format(field=field, method=method)
        
        attempts = 0
        while name in used_names and attempts < 5:
            template = random.choice(name_templates)
            field = random.choice(fields_list)
            method = random.choice(methods)
            name = template.format(field=field, method=method)
            attempts += 1
        used_names.add(name)
        
        source = random.choice(PROJECT_SOURCES)
        level = random.choice(PROJECT_LEVELS)
        status = random.choice(PROJECT_STATUSES)
        
        start_year = random.randint(2019, 2024)
        start_month = random.randint(1, 12)
        start_date = f'{start_year}-{start_month:02d}-01'
        
        end_year = start_year + random.randint(2, 5)
        end_date = f'{end_year}-12-31'
        
        if status == '结题':
            end_date = f'{start_year + random.randint(2, 3)}-12-31'
        
        # 经费
        if level == '国家级':
            fund = round(random.uniform(50, 800), 2)
        elif level == '省部级':
            fund = round(random.uniform(30, 200), 2)
        else:
            fund = round(random.uniform(20, 100), 2)
        
        projects.append({
            'name': name, 'source': source, 'level': level,
            'start_date': start_date, 'end_date': end_date,
            'fund': fund, 'status': status
        })
    
    return projects

def generate_relationships(num_researchers, num_papers, num_patents, num_projects):
    """生成关联关系"""
    # 论文-作者 (每篇1-4位作者)
    paper_researcher = []
    for p_id in range(1, num_papers + 1):
        num_authors = random.choices([1, 2, 3, 4], weights=[15, 50, 25, 10])[0]
        authors = random.sample(range(1, num_researchers + 1), num_authors)
        for order, r_id in enumerate(authors, 1):
            paper_researcher.append((p_id, r_id, order))
    
    # 专利-发明人 (每项1-3位发明人)
    patent_researcher = []
    for pat_id in range(1, num_patents + 1):
        num_inventors = random.choices([1, 2, 3], weights=[20, 55, 25])[0]
        inventors = random.sample(range(1, num_researchers + 1), num_inventors)
        for r_id in inventors:
            patent_researcher.append((pat_id, r_id))
    
    # 项目-成员 (每个项目2-5人, 含1名负责人)
    project_researcher = []
    for proj_id in range(1, num_projects + 1):
        num_members = random.choices([2, 3, 4, 5], weights=[20, 45, 25, 10])[0]
        members = random.sample(range(1, num_researchers + 1), num_members)
        for idx, r_id in enumerate(members):
            role = '负责人' if idx == 0 else '参与人'
            project_researcher.append((proj_id, r_id, role))
    
    # 合作关系 (基于共同论文/项目)
    cooperate_set = set()
    # 从论文合作者中提取
    paper_groups = {}
    for p_id, r_id, order in paper_researcher:
        paper_groups.setdefault(p_id, []).append(r_id)
    for p_id, authors in paper_groups.items():
        if len(authors) >= 2:
            for i in range(len(authors)):
                for j in range(i + 1, len(authors)):
                    pair = (min(authors[i], authors[j]), max(authors[i], authors[j]))
                    cooperate_set.add(pair)
    
    # 从项目合作者中提取
    proj_groups = {}
    for proj_id, r_id, role in project_researcher:
        proj_groups.setdefault(proj_id, []).append(r_id)
    for proj_id, members in proj_groups.items():
        if len(members) >= 2:
            for i in range(len(members)):
                for j in range(i + 1, len(members)):
                    pair = (min(members[i], members[j]), max(members[i], members[j]))
                    cooperate_set.add(pair)
    
    cooperate_with = list(cooperate_set)
    return paper_researcher, patent_researcher, project_researcher, cooperate_with

# ============================================================
# SQL 输出
# ============================================================

def write_sql(filename, institutions, researchers, papers, patents, projects,
              paper_researcher, patent_researcher, project_researcher):
    with open(filename, 'w', encoding='utf-8') as f:
        f.write("-- 科研人员画像分析系统 - 测试数据初始化脚本 (MySQL)\n")
        f.write(f"-- 数据规模：{len(institutions)}个院系/部门、{len(researchers)}个科研人员、{len(papers)}篇论文、{len(patents)}项专利、{len(projects)}个项目\n\n")
        f.write("USE `research_portrait`;\n\n")
        
        # 清空旧数据
        f.write("-- 清空旧数据\n")
        f.write("DELETE FROM `paper_researcher`;\n")
        f.write("DELETE FROM `patent_researcher`;\n")
        f.write("DELETE FROM `project_researcher`;\n")
        f.write("DELETE FROM `paper`;\n")
        f.write("DELETE FROM `patent`;\n")
        f.write("DELETE FROM `project`;\n")
        f.write("DELETE FROM `researcher`;\n")
        f.write("DELETE FROM `institution`;\n")
        f.write("DELETE FROM `weight_config`;\n\n")
        
        # 1. 机构
        f.write(f"-- 1. 插入院系/部门数据 ({len(institutions)}个)\n")
        f.write("INSERT INTO `institution` (`id`, `name`, `type`, `country`) VALUES\n")
        for i, (name, itype, country) in enumerate(institutions):
            end = ';\n' if i == len(institutions) - 1 else ',\n'
            f.write(f"({i+1}, '{name}', '{itype}', '{country}'){end}")
        f.write("\n")
        
        # 2. 科研人员
        f.write(f"-- 2. 插入科研人员数据 ({len(researchers)}个)\n")
        f.write("INSERT INTO `researcher` (`id`, `name`, `age`, `department`, `title`, `research_field`, `discipline_category`, `institution_id`, `influence_index`) VALUES\n")
        for i, r in enumerate(researchers):
            end = ';\n' if i == len(researchers) - 1 else ',\n'
            f.write(f"({i+1}, '{r['name']}', {r['age']}, '{r['department']}', '{r['title']}', '{r['field']}', '{r['discipline']}', {r['institution_id']}, {r['influence']:.2f}){end}")
        f.write("\n")
        
        # 3. 论文
        f.write(f"-- 3. 插入论文数据 ({len(papers)}篇)\n")
        f.write("INSERT INTO `paper` (`id`, `title`, `journal`, `doi`, `pub_date`, `cited_num`, `subject_field`, `impact_factor`) VALUES\n")
        for i, p in enumerate(papers):
            end = ';\n' if i == len(papers) - 1 else ',\n'
            f.write(f"({i+1}, '{p['title']}', '{p['journal']}', '{p['doi']}', '{p['pub_date']}', {p['cited']}, '{p['subject']}', {p['impact']}){end}")
        f.write("\n")
        
        # 4. 专利
        f.write(f"-- 4. 插入专利数据 ({len(patents)}项)\n")
        f.write("INSERT INTO `patent` (`id`, `title`, `patent_no`, `apply_date`, `grant_date`, `type`, `status`) VALUES\n")
        for i, pat in enumerate(patents):
            grant = f"'{pat['grant_date']}'" if pat['grant_date'] else 'NULL'
            end = ';\n' if i == len(patents) - 1 else ',\n'
            f.write(f"({i+1}, '{pat['name']}', '{pat['patent_no']}', '{pat['apply_date']}', {grant}, '{pat['patent_type']}', '{pat['status']}'){end}")
        f.write("\n")
        
        # 5. 项目
        f.write(f"-- 5. 插入项目数据 ({len(projects)}个)\n")
        f.write("INSERT INTO `project` (`id`, `name`, `source`, `level`, `start_date`, `end_date`, `total_funding`, `status`) VALUES\n")
        for i, proj in enumerate(projects):
            end = ';\n' if i == len(projects) - 1 else ',\n'
            f.write(f"({i+1}, '{proj['name']}', '{proj['source']}', '{proj['level']}', '{proj['start_date']}', '{proj['end_date']}', {proj['fund']:.2f}, '{proj['status']}'){end}")
        f.write("\n")
        
        # 6. 论文-作者关联
        f.write(f"-- 6. 插入论文作者关联 ({len(paper_researcher)}条)\n")
        f.write("INSERT INTO `paper_researcher` (`paper_id`, `researcher_id`, `author_order`) VALUES\n")
        for i, (p_id, r_id, order) in enumerate(paper_researcher):
            end = ';\n' if i == len(paper_researcher) - 1 else ',\n'
            f.write(f"({p_id}, {r_id}, {order}){end}")
        f.write("\n")
        
        # 7. 专利-发明人关联
        f.write(f"-- 7. 插入专利发明人关联 ({len(patent_researcher)}条)\n")
        f.write("INSERT INTO `patent_researcher` (`patent_id`, `researcher_id`) VALUES\n")
        for i, (pat_id, r_id) in enumerate(patent_researcher):
            end = ';\n' if i == len(patent_researcher) - 1 else ',\n'
            f.write(f"({pat_id}, {r_id}){end}")
        f.write("\n")
        
        # 8. 项目-成员关联
        f.write(f"-- 8. 插入项目成员关联 ({len(project_researcher)}条)\n")
        f.write("INSERT INTO `project_researcher` (`project_id`, `researcher_id`, `role`) VALUES\n")
        for i, (proj_id, r_id, role) in enumerate(project_researcher):
            end = ';\n' if i == len(project_researcher) - 1 else ',\n'
            f.write(f"({proj_id}, {r_id}, '{role}'){end}")
        f.write("\n")
        
        # 9. 权重配置
        f.write("-- 9. 插入权重配置数据\n")
        f.write("INSERT INTO `weight_config` (`id`, `discipline`, `paper_weight`, `patent_weight`, `project_weight`, `decay_rate`) VALUES\n")
        f.write("(1, '工学', 0.30, 0.40, 0.30, 0.05),\n")
        f.write("(2, '理学', 0.60, 0.10, 0.30, 0.05),\n")
        f.write("(3, '人文社科', 0.50, 0.10, 0.40, 0.03),\n")
        f.write("(4, '医学', 0.40, 0.20, 0.40, 0.04),\n")
        f.write("(5, '管理学', 0.35, 0.25, 0.40, 0.04);\n")

# ============================================================
# Cypher 输出
# ============================================================

def write_cypher(filename, institutions, researchers, papers, patents, projects,
                 paper_researcher, patent_researcher, project_researcher, cooperate_with):
    with open(filename, 'w', encoding='utf-8') as f:
        f.write("// 科研人员画像分析系统 - Neo4j 测试数据初始化脚本 (Cypher)\n")
        f.write(f"// 数据规模：{len(institutions)}院系/部门、{len(researchers)}科研人员、{len(papers)}论文、{len(patents)}专利、{len(projects)}项目\n\n")
        
        f.write("// 清空现有数据\n")
        f.write("MATCH (n) DETACH DELETE n;\n\n")
        
        # 约束与索引
        f.write("// 创建约束与索引\n")
        f.write("CREATE CONSTRAINT researcher_id_unique IF NOT EXISTS FOR (r:Researcher) REQUIRE r.id IS UNIQUE;\n")
        f.write("CREATE CONSTRAINT institution_id_unique IF NOT EXISTS FOR (i:Institution) REQUIRE i.id IS UNIQUE;\n")
        f.write("CREATE CONSTRAINT paper_id_unique IF NOT EXISTS FOR (p:Paper) REQUIRE p.id IS UNIQUE;\n")
        f.write("CREATE CONSTRAINT patent_id_unique IF NOT EXISTS FOR (p:Patent) REQUIRE p.id IS UNIQUE;\n")
        f.write("CREATE CONSTRAINT project_id_unique IF NOT EXISTS FOR (p:Project) REQUIRE p.id IS UNIQUE;\n")
        f.write("CREATE CONSTRAINT weightconfig_id_unique IF NOT EXISTS FOR (w:WeightConfig) REQUIRE w.id IS UNIQUE;\n")
        f.write("CREATE INDEX researcher_name_index IF NOT EXISTS FOR (r:Researcher) ON (r.name);\n")
        f.write("CREATE INDEX researcher_discipline_index IF NOT EXISTS FOR (r:Researcher) ON (r.disciplineCategory);\n\n")
        
        # 1. 机构节点 - 使用 UNWIND
        f.write(f"// 1. 创建院系/部门节点 ({len(institutions)}个)\n")
        f.write("UNWIND [\n")
        for i, (name, itype, country) in enumerate(institutions):
            end = ',' if i < len(institutions) - 1 else ''
            f.write(f"  [{i+1}, '{name}', '{itype}', '{country}']{end}\n")
        f.write("] AS row\n")
        f.write("CREATE (:Institution {id: row[0], name: row[1], type: row[2], country: row[3]});\n\n")
        
        # 2. 科研人员节点
        f.write(f"// 2. 创建科研人员节点 ({len(researchers)}个)\n")
        f.write("UNWIND [\n")
        for i, r in enumerate(researchers):
            end = ',' if i < len(researchers) - 1 else ''
            f.write(f"  [{i+1}, '{r['name']}', {r['age']}, '{r['department']}', '{r['title']}', '{r['field']}', '{r['discipline']}', {r['institution_id']}, {r['influence']:.2f}]{end}\n")
        f.write("] AS row\n")
        f.write("CREATE (:Researcher {id: row[0], name: row[1], age: row[2], department: row[3], title: row[4], researchField: row[5], disciplineCategory: row[6], influenceIndex: row[8]})\n")
        f.write("WITH row MATCH (i:Institution {id: row[7]}) MATCH (r:Researcher {id: row[0]}) CREATE (r)-[:BELONG_TO]->(i);\n\n")
        
        # 3. 论文节点
        f.write(f"// 3. 创建论文节点 ({len(papers)}篇)\n")
        f.write("UNWIND [\n")
        for i, p in enumerate(papers):
            end = ',' if i < len(papers) - 1 else ''
            f.write(f"  [{i+1}, '{p['title']}', '{p['journal']}', '{p['doi']}', '{p['pub_date']}', {p['cited']}, '{p['subject']}', {p['impact']}]{end}\n")
        f.write("] AS row\n")
        f.write("CREATE (:Paper {id: row[0], title: row[1], journal: row[2], doi: row[3], pubDate: date(row[4]), citedNum: row[5], subjectField: row[6], impactFactor: row[7]});\n\n")
        
        # 4. 专利节点 (注意: MySQL title -> Neo4j name, type -> patentType)
        f.write(f"// 4. 创建专利节点 ({len(patents)}项) - 属性映射: title→name, type→patentType\n")
        f.write("UNWIND [\n")
        for i, pat in enumerate(patents):
            grant = f"'{pat['grant_date']}'" if pat['grant_date'] else 'NULL'
            end = ',' if i < len(patents) - 1 else ''
            f.write(f"  [{i+1}, '{pat['name']}', '{pat['patent_no']}', '{pat['apply_date']}', {grant}, '{pat['patent_type']}', '{pat['status']}']{end}\n")
        f.write("] AS row\n")
        f.write("CREATE (:Patent {id: row[0], name: row[1], patentNo: row[2], applyDate: date(row[3]), grantDate: CASE WHEN row[4] IS NULL THEN NULL ELSE date(row[4]) END, patentType: row[5], status: row[6]});\n\n")
        
        # 5. 项目节点 (注意: MySQL name -> projName, level -> projLevel, total_funding -> fund)
        f.write(f"// 5. 创建项目节点 ({len(projects)}个) - 属性映射: name→projName, level→projLevel, total_funding→fund\n")
        f.write("UNWIND [\n")
        for i, proj in enumerate(projects):
            end = ',' if i < len(projects) - 1 else ''
            f.write(f"  [{i+1}, '{proj['name']}', '{proj['source']}', '{proj['level']}', '{proj['start_date']}', '{proj['end_date']}', {proj['fund']:.2f}, '{proj['status']}']{end}\n")
        f.write("] AS row\n")
        f.write("CREATE (:Project {id: row[0], projName: row[1], source: row[2], projLevel: row[3], startDate: date(row[4]), endDate: date(row[5]), fund: row[6], status: row[7]});\n\n")
        
        # 6. WRITE 关系
        f.write(f"// 6. 创建论文撰写关系 (WRITE) - {len(paper_researcher)}条\n")
        f.write("UNWIND [\n")
        for i, (p_id, r_id, order) in enumerate(paper_researcher):
            end = ',' if i < len(paper_researcher) - 1 else ''
            f.write(f"  [{r_id}, {p_id}]{end}\n")
        f.write("] AS pair\n")
        f.write("MATCH (r:Researcher {id: pair[0]}), (p:Paper {id: pair[1]})\n")
        f.write("CREATE (r)-[:WRITE]->(p);\n\n")
        
        # 7. INVENT 关系
        f.write(f"// 7. 创建专利发明关系 (INVENT) - {len(patent_researcher)}条\n")
        f.write("UNWIND [\n")
        for i, (pat_id, r_id) in enumerate(patent_researcher):
            end = ',' if i < len(patent_researcher) - 1 else ''
            f.write(f"  [{r_id}, {pat_id}]{end}\n")
        f.write("] AS pair\n")
        f.write("MATCH (r:Researcher {id: pair[0]}), (p:Patent {id: pair[1]})\n")
        f.write("CREATE (r)-[:INVENT]->(p);\n\n")
        
        # 8. CHARGE 关系
        charge_rels = [(r_id, proj_id) for proj_id, r_id, role in project_researcher if role == '负责人']
        f.write(f"// 8. 创建项目负责关系 (CHARGE) - {len(charge_rels)}条\n")
        f.write("UNWIND [\n")
        for i, (r_id, proj_id) in enumerate(charge_rels):
            end = ',' if i < len(charge_rels) - 1 else ''
            f.write(f"  [{r_id}, {proj_id}]{end}\n")
        f.write("] AS pair\n")
        f.write("MATCH (r:Researcher {id: pair[0]}), (p:Project {id: pair[1]})\n")
        f.write("CREATE (r)-[:`CHARGE`]->(p);\n\n")
        
        # 9. PARTICIPATE 关系
        participate_rels = [(r_id, proj_id) for proj_id, r_id, role in project_researcher if role == '参与人']
        f.write(f"// 9. 创建项目参与关系 (PARTICIPATE) - {len(participate_rels)}条\n")
        f.write("UNWIND [\n")
        for i, (r_id, proj_id) in enumerate(participate_rels):
            end = ',' if i < len(participate_rels) - 1 else ''
            f.write(f"  [{r_id}, {proj_id}]{end}\n")
        f.write("] AS pair\n")
        f.write("MATCH (r:Researcher {id: pair[0]}), (p:Project {id: pair[1]})\n")
        f.write("CREATE (r)-[:PARTICIPATE]->(p);\n\n")
        
        # 10. COOPERATE_WITH 关系
        f.write(f"// 10. 创建合作者关系 (COOPERATE_WITH) - {len(cooperate_with)}条\n")
        f.write("UNWIND [\n")
        for i, (r1_id, r2_id) in enumerate(cooperate_with):
            end = ',' if i < len(cooperate_with) - 1 else ''
            f.write(f"  [{r1_id}, {r2_id}]{end}\n")
        f.write("] AS pair\n")
        f.write("MATCH (r1:Researcher {id: pair[0]}), (r2:Researcher {id: pair[1]})\n")
        f.write("CREATE (r1)-[:COOPERATE_WITH]->(r2);\n\n")
        
        # 11. 权重配置
        f.write("// 11. 创建权重配置节点 (WeightConfig)\n")
        f.write("CREATE (wc1:WeightConfig {id: 1, discipline: '工学', paperWeight: 0.30, patentWeight: 0.40, projectWeight: 0.30, decayRate: 0.05}),\n")
        f.write("(wc2:WeightConfig {id: 2, discipline: '理学', paperWeight: 0.60, patentWeight: 0.10, projectWeight: 0.30, decayRate: 0.05}),\n")
        f.write("(wc3:WeightConfig {id: 3, discipline: '人文社科', paperWeight: 0.50, patentWeight: 0.10, projectWeight: 0.40, decayRate: 0.03}),\n")
        f.write("(wc4:WeightConfig {id: 4, discipline: '医学', paperWeight: 0.40, patentWeight: 0.20, projectWeight: 0.40, decayRate: 0.04}),\n")
        f.write("(wc5:WeightConfig {id: 5, discipline: '管理学', paperWeight: 0.35, patentWeight: 0.25, projectWeight: 0.40, decayRate: 0.04});\n")

# ============================================================
# 主函数
# ============================================================

def main():
    import os
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    print("=" * 60)
    print("科研人员画像分析系统 - 测试数据生成器")
    print("=" * 60)
    
    print("\n[1/6] 生成院系数据...")
    institutions = generate_institutions()
    print(f"  -> {len(institutions)} 个院系/部门")
    
    print("[2/6] 生成科研人员数据...")
    researchers = generate_researchers(institutions)
    print(f"  -> {len(researchers)} 个科研人员")
    
    print("[3/6] 生成论文数据...")
    papers = generate_papers()
    print(f"  -> {len(papers)} 篇论文")
    
    print("[4/6] 生成专利数据...")
    patents = generate_patents()
    print(f"  -> {len(patents)} 项专利")
    
    print("[5/6] 生成项目数据...")
    projects = generate_projects()
    print(f"  -> {len(projects)} 个项目")
    
    print("[6/6] 生成关联关系...")
    paper_r, patent_r, project_r, cooperate = generate_relationships(
        len(researchers), len(papers), len(patents), len(projects))
    print(f"  -> 论文-作者: {len(paper_r)} 条")
    print(f"  -> 专利-发明人: {len(patent_r)} 条")
    print(f"  -> 项目-成员: {len(project_r)} 条")
    print(f"  -> 合作关系: {len(cooperate)} 条")
    
    print("\n写入 MySQL 脚本...")
    sql_file = os.path.join(script_dir, 'test-data.sql')
    write_sql(sql_file, institutions, researchers, papers, patents, projects,
              paper_r, patent_r, project_r)
    sql_size = os.path.getsize(sql_file)
    print(f"  -> {sql_file} ({sql_size / 1024:.1f} KB)")
    
    print("\n写入 Neo4j Cypher 脚本...")
    cypher_file = os.path.join(script_dir, 'neo4j-test-data.cypher')
    write_cypher(cypher_file, institutions, researchers, papers, patents, projects,
                 paper_r, patent_r, project_r, cooperate)
    cypher_size = os.path.getsize(cypher_file)
    print(f"  -> {cypher_file} ({cypher_size / 1024:.1f} KB)")
    
    print("\n" + "=" * 60)
    print("数据生成完成!")
    print(f"  院系/部门: {len(institutions):>5} 个")
    print(f"  科研人员: {len(researchers):>5} 个")
    print(f"  论文:     {len(papers):>5} 篇")
    print(f"  专利:     {len(patents):>5} 项")
    print(f"  项目:     {len(projects):>5} 个")
    print(f"  关联关系: {len(paper_r) + len(patent_r) + len(project_r) + len(cooperate):>5} 条")
    print("=" * 60)

if __name__ == '__main__':
    main()
