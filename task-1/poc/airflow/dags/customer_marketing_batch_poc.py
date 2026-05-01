import csv
from datetime import timedelta
from pathlib import Path
from typing import Any

import psycopg2
from airflow import DAG
from airflow.decorators import task
from airflow.exceptions import AirflowException, AirflowFailException
from airflow.hooks.base import BaseHook
from airflow.operators.email import EmailOperator
from airflow.operators.empty import EmptyOperator
from airflow.operators.python import get_current_context
from airflow.utils.trigger_rule import TriggerRule
from pendulum import datetime

DATA_DIR = Path("/opt/airflow/include/data")
CSV_PATH = DATA_DIR / "delivery_statuses.csv"
LATE_THRESHOLD = 0.30


def _get_demo_conf() -> dict[str, Any]:
    context = get_current_context()
    dag_run = context.get("dag_run")
    if dag_run and dag_run.conf:
        return dict(dag_run.conf)
    return {}


default_args = {
    "owner": "marketing-platform",
    "depends_on_past": False,
    "email": ["marketing-alerts@example.local"],
    "email_on_failure": False,
    "email_on_retry": False,
    "retries": 2,
    "retry_delay": timedelta(seconds=20),
    "retry_exponential_backoff": True,
}


with DAG(
    dag_id="customer_marketing_batch_poc",
    description="POC пакетного маркетингового пайплайна на Apache Airflow",
    start_date=datetime(2025, 1, 1),
    schedule=None,
    catchup=False,
    default_args=default_args,
    tags=["marketing", "batch", "poc"],
) as dag:
    @task(task_id="load_delivery_statuses")
    def load_delivery_statuses() -> list[dict[str, Any]]:
        context = get_current_context()
        demo_conf = _get_demo_conf()
        if demo_conf.get("simulate_retry") and context["ti"].try_number == 1:
            raise AirflowException("Симулируем временную ошибку чтения CSV для демонстрации retry.")

        with CSV_PATH.open(encoding="utf-8") as csv_file:
            rows = list(csv.DictReader(csv_file))

        return rows

    @task(task_id="load_orders_from_postgres")
    def load_orders_from_postgres() -> list[dict[str, Any]]:
        connection = BaseHook.get_connection("marketing_postgres")
        with psycopg2.connect(
            host=connection.host,
            port=connection.port,
            dbname=connection.schema,
            user=connection.login,
            password=connection.password,
        ) as db_connection:
            with db_connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT
                        order_id,
                        payment_status
                    FROM marketing.orders
                    ORDER BY order_id
                    """
                )
                rows = cursor.fetchall()

        return [
            {
                "order_id": order_id,
                "payment_status": payment_status,
            }
            for order_id, payment_status in rows
        ]

    @task(task_id="build_customer_summary")
    def build_customer_summary(
        delivery_statuses: list[dict[str, Any]], orders: list[dict[str, Any]]
    ) -> dict[str, Any]:
        demo_conf = _get_demo_conf()
        if demo_conf.get("force_failure"):
            raise AirflowFailException("Симулируем аварийное завершение для демонстрации failure-email.")

        orders_by_id = {order["order_id"]: order for order in orders}
        rows_count = 0
        late_count = 0
        unpaid_count = 0

        for delivery in delivery_statuses:
            order = orders_by_id.get(int(delivery["order_id"]))
            if not order:
                continue

            rows_count += 1
            if delivery["delivery_status"] == "late":
                late_count += 1
            if order["payment_status"] != "paid":
                unpaid_count += 1

        late_ratio = round(late_count / rows_count, 2) if rows_count else 0.0

        return {
            "rows_count": rows_count,
            "late_orders_count": late_count,
            "late_ratio": late_ratio,
            "unpaid_orders_count": unpaid_count,
            "needs_attention": late_ratio >= LATE_THRESHOLD or unpaid_count > 0,
        }

    @task.branch(task_id="choose_branch")
    def choose_branch(summary: dict[str, Any]) -> str:
        if summary["needs_attention"]:
            return "attention_path"
        return "regular_path"

    attention_path = EmptyOperator(task_id="attention_path")
    regular_path = EmptyOperator(task_id="regular_path")

    success_join = EmptyOperator(
        task_id="success_join",
        trigger_rule=TriggerRule.NONE_FAILED_MIN_ONE_SUCCESS,
    )

    notify_success = EmailOperator(
        task_id="notify_success",
        to="marketing-team@example.local",
        subject="[SUCCESS] customer_marketing_batch_poc / {{ run_id }}",
        html_content="""
        <h3>Пайплайн завершён успешно</h3>
        <p>DAG: {{ dag.dag_id }}</p>
        <p>Run ID: {{ run_id }}</p>
        <p>Проверьте MailHog и статус задач в Airflow UI.</p>
        """,
    )

    notify_failure = EmailOperator(
        task_id="notify_failure",
        to="marketing-team@example.local",
        subject="[FAILED] customer_marketing_batch_poc / {{ run_id }}",
        html_content="""
        <h3>Пайплайн завершился с ошибкой</h3>
        <p>DAG: {{ dag.dag_id }}</p>
        <p>Run ID: {{ run_id }}</p>
        <p>Это демонстрационное письмо; детали ошибки доступны в логах Airflow.</p>
        """,
        trigger_rule=TriggerRule.ONE_FAILED,
    )

    delivery_statuses = load_delivery_statuses()
    orders = load_orders_from_postgres()
    summary = build_customer_summary(delivery_statuses, orders)
    branch = choose_branch(summary)

    [delivery_statuses, orders] >> summary >> branch
    branch >> [attention_path, regular_path]
    [attention_path, regular_path] >> success_join >> notify_success
    [delivery_statuses, orders, summary, attention_path, regular_path, success_join] >> notify_failure
