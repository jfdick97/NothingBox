"""Check language coverage, format arguments, resource references, and JVM fallbacks.
Run --update-fallback after editing the default German strings.
"""
from pathlib import Path
from collections import Counter
import argparse
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
SRC = ROOT / 'app/src/main/java/de/jfd/straingauge'


def read_strings(path):
    result = {}
    translated = set()
    for node in ET.parse(path).getroot().findall('string'):
        name = node.attrib['name']
        assert name not in result, f'Duplicate resource: {name}'
        raw = ''.join(node.itertext())
        if raw.startswith('"') and raw.endswith('"'):
            raw = raw[1:-1]
        # Android resource escapes, not XML escapes (ElementTree resolved those).
        raw = re.sub(r'\\([\\\'"nrt])', lambda m: {
            '\\': '\\', "'": "'", '"': '"', 'n': '\n', 'r': '\r', 't': '\t'
        }[m.group(1)], raw)
        result[name] = raw
        if node.attrib.get('translatable') != 'false':
            translated.add(name)
    return result, translated


def fallback_source(strings):
    lines = [
        'package de.jfd.straingauge', '',
        '// Generated from res/values/strings.xml. Run tools/check_localization.py --update-fallback.',
        'internal object DefaultStrings {',
        '    val values: Map<Int, String> = mapOf('
    ]
    for name, value in sorted(strings.items()):
        literal = json.dumps(value, ensure_ascii=False).replace('$', '\\$')
        lines.append(f'        R.string.{name} to {literal},')
    lines.extend(['    )', '}'])
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--update-fallback', action='store_true')
    args = parser.parse_args()
    german, required = read_strings(RES / 'values/strings.xml')
    english, _ = read_strings(RES / 'values-en/strings.xml')
    assert required == set(english), f'English coverage differs: {required ^ set(english)}'
    for name in required:
        placeholders = lambda value: Counter(re.findall(r'%\d+\$[sdf]', value))
        assert placeholders(german[name]) == placeholders(english[name]), f'Format mismatch: {name}'
    for path in SRC.glob('*.kt'):
        for name in re.findall(r'R\.string\.([a-zA-Z0-9_]+)', path.read_text()):
            assert name in german, f'Unknown string {name} in {path.name}'
    expected = fallback_source(german)
    fallback = SRC / 'DefaultStrings.kt'
    if args.update_fallback:
        fallback.write_text(expected)
    assert fallback.read_text() == expected, 'Run tools/check_localization.py --update-fallback'
    for path in RES.rglob('*.xml'):
        ET.parse(path)
    print(f'PASS: {len(german)} default strings; {len(english)} English translations; placeholders, references, XML, and JVM fallback agree.')


if __name__ == '__main__':
    main()
