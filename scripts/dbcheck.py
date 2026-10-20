import sqlite3, sys
sys.stdout.reconfigure(encoding="utf-8")
con = sqlite3.connect(r"artifacts\db_dump.db")
for row in con.execute("SELECT id, protections, customProtections FROM records ORDER BY id DESC LIMIT 8"):
    print(row)
