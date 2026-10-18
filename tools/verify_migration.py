"""Execute the actual additive migration SQL using SQLite on the host."""
import re
import sqlite3
from pathlib import Path
source=(Path(__file__).resolve().parents[1]/'app/src/main/java/de/jfd/straingauge/Data.kt').read_text()
sql=re.findall(r'db.execSQL\("(ALTER TABLE days[^"\n]+)"\)',source)
assert len(sql)==2
with sqlite3.connect(':memory:') as db:
    db.execute('CREATE TABLE days(date TEXT NOT NULL PRIMARY KEY,demands REAL,tension REAL,energy REAL,agency REAL,factors INTEGER NOT NULL,scanId TEXT,detected TEXT,updatedAt INTEGER NOT NULL)')
    db.execute("INSERT INTO days VALUES ('2026-10-12',0,NULL,7.3,10,5,NULL,NULL,1234)")
    for statement in sql:db.execute(statement)
    row=db.execute('SELECT * FROM days').fetchone()
    assert row==('2026-10-12',0.,None,7.3,10.,5,None,None,1234,'',''),row
    columns={r[1]:r for r in db.execute('PRAGMA table_info(days)')}
    for name in ['custom1','custom2']:
        assert columns[name][2]=='TEXT' and columns[name][3]==1 and columns[name][4]=="''"
print('PASS actual migration SQL: existing values, NULL and zero preserved')
