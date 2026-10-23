"""Run with Serena's isolated Python environment; no edits or game launches."""

import asyncio
import json
import os
import re
import time
from pathlib import Path

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client


async def main():
    root = Path(__file__).resolve().parents[2]
    executable = Path.home() / '.local/bin/serena.exe'
    env = dict(os.environ)
    env['JAVA_TOOL_OPTIONS'] = (env.get('JAVA_TOOL_OPTIONS', '')
                                + ' -Djdk.net.unixdomain.tmpdir=C:/Windows/Temp').strip()
    params = StdioServerParameters(command=str(executable), args=[
        'start-mcp-server', '--context', 'codex', '--agent-interface', 'tools',
        '--enable-web-dashboard', 'false'], env=env)
    report = {'project': str(root), 'calls': []}

    async def call(session, name, arguments):
        start = time.perf_counter()
        result = await asyncio.wait_for(session.call_tool(name, arguments), 120)
        texts = [c.text for c in result.content if hasattr(c, 'text')]
        is_error = bool(result.is_error)
        report['calls'].append({'tool': name, 'seconds': round(time.perf_counter() - start, 3),
                                'is_error': is_error, 'text': texts})
        print(name, report['calls'][-1]['seconds'], is_error, flush=True)
        if is_error:
            raise RuntimeError(str(texts)[:1500])
        return texts

    try:
        async with stdio_client(params) as (read, write):
            async with ClientSession(read, write) as session:
                await asyncio.wait_for(session.initialize(), 45)
                tool_list = await session.list_tools()
                report['tool_schemas'] = [tool.model_dump(by_alias=True) for tool in tool_list.tools]
                instructions = await call(session, 'initial_instructions', {})
                report['instructions'] = instructions
                session_match = re.search(r'Your Serena session id is `([^`]+)`', '\n'.join(instructions))
                if not session_match:
                    raise RuntimeError('Session id missing from instructions')
                await call(session, 'activate_project', {'project': str(root), 'session': session_match.group(1)})
                for _ in range(2):
                    symbols = await call(session, 'find_symbol', {
                        'name_path_pattern': 'LateMixinsLoader/getMixins',
                        'relative_path': 'src/main/java/com/xyp/gtnotgood/loader/LateMixinsLoader.java',
                        'include_body': True})
                    if 'getMixins' not in ''.join(symbols) or 'loadedMods' not in ''.join(symbols):
                        raise RuntimeError('Symbol body missing from response')
                references = await call(session, 'find_referencing_symbols', {
                    'name_path': 'GTNGMultiBlockBase<T extends GTNGMultiBlockBase<T>>',
                    'relative_path': 'src/main/java/com/xyp/gtnotgood/common/machines/multiblock/multiMachineBase/GTNGMultiBlockBase.java'})
                if not any(text.strip() not in ('', '[]', '{}') for text in references):
                    raise RuntimeError('No references returned')
                report['passed'] = True
    except Exception as error:
        report['passed'] = False
        report['error'] = repr(error)
        raise
    finally:
        (root / '.gradle/codex-serena-smoke.json').write_text(
            json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')


if __name__ == '__main__':
    asyncio.run(main())
