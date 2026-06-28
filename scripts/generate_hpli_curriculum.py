#!/usr/bin/env python3
"""Generate a JD-grounded, staged MemoSpark curriculum as auditable JSON."""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


JD = """
目标岗位：高级 SRE / 平台工程

核心职责：
- 建设 Internal Developer Platform，标准化 Jenkins Shared Library、GitLab CI、
  ArgoCD/FluxCD 等 CI/CD 与 GitOps 流程，保障多环境一致性。
- 规划、部署、升级与调优大规模自建 Kubernetes 集群；排查 Calico/Cilium、
  BGP/VXLAN、CSI、调度、QoS、在线与 Flink/Spark 混部问题。
- 保障 Flink on Kubernetes、HDFS/YARN、Kafka、ZooKeeper 等生产系统，
  处理反压、Checkpoint、RocksDB、ISR、Rebalance、NameNode HA 和容量问题。
- 建设 Prometheus、Grafana、Loki/ELK、SkyWalking 可观测体系和 SLI/SLO，
  承担 On-call、事故复盘和故障自愈。
- 使用 Go 或 Python 开发 Operator、CMDB、发布系统与告警平台。
- 加分项包括 MySQL/PostgreSQL/Redis 高可用、Java 构建优化、GPU/AI 推理服务、
  国产化 OS 和 CPU 适配。
""".strip()


DECKS = [
    {
        "name": "Kubernetes 深度运维",
        "description": "Kubernetes 控制面、调度、网络、存储、升级和生产故障处置。",
        "count": 32,
        "weight": 5,
        "topics": [
            "API Server、etcd、Scheduler、Controller Manager、Kubelet 的职责与交互",
            "Pod、ReplicaSet、Deployment、StatefulSet、DaemonSet 的演进与边界",
            "Calico/Cilium、BGP、VXLAN、eBPF、NetworkPolicy 和跨节点网络排障",
            "CSI、PV/PVC、StorageClass、VolumeAttachment 和挂载故障",
            "requests/limits、QoS、优先级、抢占、亲和性、污点和混部隔离",
            "集群升级、证书、etcd 备份恢复、离线镜像和内核参数",
            "Helm 发布、回滚、CRD/Operator 以及生产级故障定位",
        ],
    },
    {
        "name": "CI/CD 与 DevOps 平台建设",
        "description": "研发效能平台、流水线、制品安全、GitOps 和多环境一致性。",
        "count": 26,
        "weight": 4,
        "topics": [
            "Jenkins Shared Library、Groovy、流水线模板与可测试性",
            "GitLab CI 的 DAG、缓存、制品、Runner 隔离和并发治理",
            "ArgoCD/FluxCD 的 GitOps 对账、漂移检测、回滚和权限边界",
            "从提交、构建、测试、镜像扫描、签名到发布的供应链",
            "多环境配置、Secret、数据库变更和渐进式发布",
            "Maven/Gradle 缓存、分层构建、远程缓存和构建可观测性",
            "IDP 的自服务接口、Golden Path、平台 API 和效能指标",
        ],
    },
    {
        "name": "运维开发语言（Go/Python）",
        "description": "面向 Operator、自动化平台与生产工具的 Go/Python 工程能力。",
        "count": 26,
        "weight": 4,
        "topics": [
            "Go goroutine、channel、context、调度器、内存模型和竞态",
            "Go HTTP/RPC、错误处理、资源释放、pprof 和工程测试",
            "Python 异常、上下文管理器、并发、类型标注和包管理",
            "client-go、controller-runtime、reconcile、finalizer 和幂等",
            "CMDB、发布系统、告警平台的数据模型与任务执行架构",
            "etcd、Kubernetes、Docker/Containerd SDK 的可靠调用",
            "运维工具的超时、重试、限流、审计、权限和可观测性",
        ],
    },
    {
        "name": "Flink 运维与调优",
        "description": "Flink on Kubernetes 的资源、状态、Checkpoint、反压和生产稳定性。",
        "count": 28,
        "weight": 4,
        "topics": [
            "JobManager、TaskManager、Slot、ResourceManager 和 K8s 部署模式",
            "Flink 内存模型、Managed Memory、Direct Memory 和容器 OOM",
            "Checkpoint barrier、对齐/非对齐、超时和失败定位",
            "RocksDB State Backend、增量 Checkpoint、本地磁盘和远端存储",
            "反压链路、吞吐/延迟、网络 buffer、水位线和数据倾斜",
            "Savepoint、升级迁移、Schema 兼容、重启策略和 HA",
            "TB 级作业容量规划、监控指标、Runbook 与典型事故",
        ],
    },
    {
        "name": "Kafka 运维与高可用架构",
        "description": "Kafka 副本、ISR、Rebalance、性能、跨机房和故障恢复。",
        "count": 22,
        "weight": 3,
        "topics": [
            "Broker、Controller/KRaft、分区、副本、AR/ISR/HW/LEO",
            "Producer acks、幂等、事务、批处理、压缩和顺序性",
            "Consumer Group、协调器、offset、Rebalance 协议和 Lag",
            "磁盘、页缓存、网络、分区数、数据倾斜和容量规划",
            "Leader 选举、Unclean Election、数据丢失和故障恢复",
            "MirrorMaker 2、Cluster Linking、跨机房 RPO/RTO",
            "Kafka 与 Flink 集成的 Exactly-once 和背压联动",
        ],
    },
    {
        "name": "HDFS/Hadoop 运维",
        "description": "HDFS/YARN 的 HA、数据可靠性、资源调度和容量治理。",
        "count": 22,
        "weight": 3,
        "topics": [
            "NameNode、DataNode、Block、Replica、读写与故障恢复",
            "QJM、JournalNode、ZKFC、Failover Controller 和 fencing",
            "小文件、FsImage/EditLog、NameNode 内存和 Federation",
            "DataNode 掉线、坏盘、欠副本、Corrupt Block 和 Decommission",
            "Balancer、Disk Balancer、容量水位和机架感知",
            "YARN RM/NM/AM、队列、资源碎片、Node Label 和抢占",
            "Hadoop 与 Kubernetes 混部、数据本地性和隔离",
        ],
    },
    {
        "name": "可观测性与SRE实践",
        "description": "指标、日志、链路、SLI/SLO、告警、On-call 和故障自愈。",
        "count": 28,
        "weight": 4,
        "topics": [
            "Prometheus 数据模型、采集、服务发现、Recording Rule 和远端存储",
            "Counter/Gauge/Histogram、标签基数、PromQL 和容量",
            "Grafana、Alertmanager、分组、抑制、静默和通知治理",
            "Loki/ELK、日志结构化、索引、标签和查询性能",
            "SkyWalking/OpenTelemetry、Trace、采样和延迟拆解",
            "SLI/SLO、Error Budget、多窗口燃烧率和用户影响告警",
            "On-call、Runbook、事件指挥、Post-mortem 和自动化自愈",
        ],
    },
    {
        "name": "数据库与中间件高可用",
        "description": "MySQL、PostgreSQL、Redis 与 ZooKeeper 的高可用和性能故障。",
        "count": 24,
        "weight": 4,
        "topics": [
            "MySQL 复制、GTID、主从延迟、MHA/Orchestrator 和切换",
            "InnoDB 事务、锁、死锁、MVCC、Redo/Undo 和慢 SQL",
            "PostgreSQL WAL、流复制、Patroni、etcd 和脑裂防护",
            "Redis RDB/AOF、内存淘汰、复制、Sentinel 和 Cluster",
            "Codis 代理、槽位迁移、扩缩容和故障处理",
            "ZooKeeper ZAB、Session、Watcher、选举和运维风险",
            "容量、备份恢复、RPO/RTO、演练和变更治理",
        ],
    },
    {
        "name": "Linux、网络与容器底层",
        "description": "Linux 性能、TCP/TLS、namespace/cgroup、容器运行时与 CNI 基础。",
        "count": 24,
        "weight": 3,
        "topics": [
            "CPU、load、调度、上下文切换、软中断和性能定位",
            "内存、page cache、swap、OOM、NUMA 和 cgroup v2",
            "文件系统、inode、IO 栈、iostat、blktrace 和坏盘",
            "TCP 握手/挥手、重传、拥塞、TIME_WAIT、连接跟踪和抓包",
            "DNS、HTTP/2/3、TLS、四层/七层负载均衡和超时",
            "namespace、cgroup、overlayfs、OCI、containerd 和 runc",
            "iptables/IPVS/eBPF、veth、bridge、路由和 VXLAN",
        ],
    },
    {
        "name": "JVM 与 Java 运行时排障",
        "description": "面向 Flink、Jenkins 等 Java 系统的 JVM、GC、线程和构建排障。",
        "count": 18,
        "weight": 2,
        "topics": [
            "堆、元空间、直接内存、线程栈和容器内存边界",
            "G1/ZGC、Young/Mixed/Full GC、停顿和日志分析",
            "JMM、volatile、锁、线程池、死锁和 CPU 飙高",
            "类加载、双亲委派、SPI、依赖冲突和 Metaspace",
            "jcmd/jstack/jmap/async-profiler/NMT 的诊断路径",
            "Maven/Gradle 依赖、缓存、测试并行和容器分层构建",
        ],
    },
]


STAGE_GUIDANCE = {
    "FOUNDATION": (
        "建立准确术语、组件职责和基本数据流。问题不能只问定义，至少要求解释一个关键因果。"
    ),
    "ADVANCED": (
        "考察机制、边界、权衡、关键参数和跨组件联动。答案要指出常见误区与适用条件。"
    ),
    "PRACTICE": (
        "采用生产故障、容量规划、升级变更或架构决策场景。答案必须给出分层排查顺序、"
        "证据、止损动作、根因验证和长期改进。"
    ),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--only", action="append", default=[], help="Generate only matching deck names")
    parser.add_argument("--max-attempts", type=int, default=4)
    return parser.parse_args()


def stage_counts(total: int) -> dict[str, int]:
    foundation = max(4, round(total * 0.30))
    advanced = max(5, round(total * 0.35))
    practice = total - foundation - advanced
    return {"FOUNDATION": foundation, "ADVANCED": advanced, "PRACTICE": practice}


def endpoint() -> str:
    value = os.environ.get("AI_API_URL", "https://token.sensenova.cn/v1")
    value = value.rstrip("/")
    return value if value.endswith("/chat/completions") else value + "/chat/completions"


def call_ai(prompt: str) -> str:
    key = os.environ.get("AI_API_KEY", "")
    if not key:
        raise RuntimeError("AI_API_KEY is required")
    body = json.dumps(
        {
            "model": os.environ.get("AI_API_MODEL", "deepseek-v4-flash"),
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.25,
        },
        ensure_ascii=False,
    ).encode("utf-8")
    request = urllib.request.Request(
        endpoint(),
        data=body,
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            payload = json.load(response)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")[:1000]
        raise RuntimeError(f"AI HTTP {error.code}: {detail}") from error
    return payload["choices"][0]["message"]["content"]


def extract_json_array(text: str) -> list[dict]:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
        cleaned = re.sub(r"\s*```$", "", cleaned)
    start = cleaned.find("[")
    end = cleaned.rfind("]")
    if start < 0 or end <= start:
        raise ValueError("No JSON array found")
    value = json.loads(cleaned[start : end + 1])
    if not isinstance(value, list):
        raise ValueError("AI result is not an array")
    return value


def normalize_card(card: dict, stage: str, order: int, seen: set[str]) -> dict | None:
    front = str(card.get("front", "")).strip()
    back = str(card.get("back", "")).strip()
    key = re.sub(r"\s+", "", front).lower()
    if not front or len(front) < 8 or key in seen:
        return None
    minimum = 150 if stage == "PRACTICE" else 110
    if len(back) < minimum:
        return None
    difficulty = str(card.get("difficulty", "")).upper()
    allowed = {
        "FOUNDATION": {"EASY", "MEDIUM"},
        "ADVANCED": {"MEDIUM", "HARD"},
        "PRACTICE": {"MEDIUM", "HARD"},
    }[stage]
    if difficulty not in allowed:
        difficulty = "HARD" if stage == "PRACTICE" else "MEDIUM"
    tags = str(card.get("tags", "")).strip()
    seen.add(key)
    return {
        "front": front,
        "back": back,
        "tags": tags,
        "contentDifficulty": difficulty,
        "learningStage": stage,
        "stageOrder": order,
        "governanceNote": str(card.get("rationale", "")).strip()[:500],
    }


def build_prompt(
    deck: dict,
    stage: str,
    needed: int,
    existing_fronts: list[str],
) -> str:
    previous = "\n".join(f"- {front}" for front in existing_fronts[-40:]) or "(none)"
    return f"""
你是资深 SRE、平台工程师和技术面试官。请为 MemoSpark 生成 {needed} 张中文主动回忆卡片。

目标 JD：
{JD}

牌组：{deck['name']}
牌组定位：{deck['description']}
覆盖主题：
{chr(10).join('- ' + topic for topic in deck['topics'])}

本批阶段：{stage}
阶段要求：{STAGE_GUIDANCE[stage]}

质量要求：
1. 问题必须具体、可判分、可在面试中出现；不要问“请介绍一下”这种无边界大题。
2. 答案必须能直接用于复习，不能一笔带过。至少包括：
   - 先给结论或排查优先级；
   - 解释关键机制与证据；
   - 指出边界、误区或失败模式；
   - 对实战题给出止损、验证和长期改进。
3. FOUNDATION/ADVANCED 答案至少 110 个中文字符，PRACTICE 至少 150 个中文字符。
4. 不要重复已有问题，不要编造命令输出或版本事实。
5. tags 用 2-4 个英文逗号分隔标签。
6. difficulty 只能是 EASY、MEDIUM、HARD，并符合阶段难度。

已有问题：
{previous}

只返回 JSON 数组，不要 markdown 代码块：
[
  {{
    "front": "具体问题",
    "back": "完整答案，可包含换行和编号",
    "tags": "k8s,scheduling",
    "difficulty": "MEDIUM",
    "rationale": "为何属于该阶段，最多一句"
  }}
]
""".strip()


def generate_deck(deck: dict, max_attempts: int) -> dict:
    cards: list[dict] = []
    seen: set[str] = set()
    next_order = 1
    for stage, target in stage_counts(deck["count"]).items():
        stage_cards: list[dict] = []
        attempts = 0
        while len(stage_cards) < target and attempts < max_attempts:
            attempts += 1
            needed = target - len(stage_cards)
            prompt = build_prompt(deck, stage, needed, [card["front"] for card in cards + stage_cards])
            response = call_ai(prompt)
            raw_cards = extract_json_array(response)
            for raw in raw_cards:
                normalized = normalize_card(raw, stage, next_order, seen)
                if normalized is None:
                    continue
                stage_cards.append(normalized)
                next_order += 1
                if len(stage_cards) >= target:
                    break
            if len(stage_cards) < target:
                time.sleep(1)
        if len(stage_cards) != target:
            raise RuntimeError(
                f"{deck['name']} {stage}: expected {target}, got {len(stage_cards)}"
            )
        cards.extend(stage_cards)
    return {
        "name": deck["name"],
        "description": deck["description"],
        "targetCount": deck["count"],
        "weight": deck["weight"],
        "topics": deck["topics"],
        "cards": cards,
    }


def main() -> int:
    args = parse_args()
    selected = [
        deck for deck in DECKS
        if not args.only or any(value in deck["name"] for value in args.only)
    ]
    result = {
        "username": "hpli",
        "targetTitle": "高级SRE",
        "factSource": JD,
        "generatedAt": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "decks": [],
    }
    for index, deck in enumerate(selected, start=1):
        print(f"[{index}/{len(selected)}] generating {deck['name']} ({deck['count']} cards)", flush=True)
        result["decks"].append(generate_deck(deck, args.max_attempts))
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    total = sum(len(deck["cards"]) for deck in result["decks"])
    shortest = min(len(card["back"]) for deck in result["decks"] for card in deck["cards"])
    print(f"generated={total} shortest_answer={shortest} output={args.output}", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())

