import csv
from datetime import datetime, timezone
from pathlib import Path

import psycopg

DATABASE_URL = "postgresql://app:app@postgres:5432/logistics"
OUTPUT_DIR = Path("/exports")
TABLE_NAME = "clients"


OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
output_file = OUTPUT_DIR / f"{TABLE_NAME}_{timestamp}.csv"

with psycopg.connect(DATABASE_URL) as connection:
    with connection.cursor() as cursor:
        cursor.execute("SELECT * FROM clients")
        headers = [column.name for column in cursor.description]
        rows = cursor.fetchall()

with output_file.open("w", newline="", encoding="utf-8") as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(headers)
    writer.writerows(rows)

print(f"Export completed successfully. Rows exported: {len(rows)}")
