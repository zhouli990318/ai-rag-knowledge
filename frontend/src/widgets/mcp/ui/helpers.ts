export type ParameterRow = {
  key: string; name: string; type: string; description: string; required: boolean;
};

export const DEFAULT_PARAMETER_SCHEMA = '{\n  "type": "object",\n  "properties": {},\n  "required": []\n}';

export function parseParameterRows(parameterSchema?: string): ParameterRow[] {
  try {
    const parsed = JSON.parse(parameterSchema || DEFAULT_PARAMETER_SCHEMA) as {
      properties?: Record<string, { type?: string; description?: string }>;
      required?: string[];
    };
    const props = parsed.properties || {};
    const req = new Set(parsed.required || []);
    return Object.entries(props).map(([name, v], i) => ({
      key: `${name}-${i}`, name, type: v?.type || 'string', description: v?.description || '', required: req.has(name),
    }));
  } catch { return []; }
}

export function buildParameterSchema(rows: ParameterRow[]): string {
  const properties = rows.reduce<Record<string, { type: string; description: string }>>((acc, r) => {
    const n = r.name.trim();
    if (!n) return acc;
    acc[n] = { type: r.type || 'string', description: r.description || '' };
    return acc;
  }, {});
  const required = rows.filter((r) => r.required && r.name.trim()).map((r) => r.name.trim());
  return JSON.stringify({ type: 'object', properties, required }, null, 2);
}
