const labels: Record<string, Record<string, string>> = {
    hu: { EASY: 'Könnyű', MEDIUM: 'Közepes', HARD: 'Nehéz', UNKNOWN: 'Ismeretlen' }
};

export function translateDifficulty(value: string | null | undefined, locale = 'hu'): string {
    if (!value) return labels[locale]?.UNKNOWN ?? labels.hu.UNKNOWN;
    return labels[locale]?.[value] ?? labels.hu[value] ?? value;
}
