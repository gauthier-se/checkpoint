# CheckPoint — Présentation (Slidev)

Support de soutenance, construit avec [Slidev](https://sli.dev/). Le thème reprend
les couleurs et le logo de l'application web.

## Lancer

```bash
pnpm install
pnpm dev      # http://localhost:3030
```

Autres commandes : `pnpm build` (SPA statique dans `dist/`), `pnpm export` (PDF/PPTX).

## Structure

| Fichier / dossier | Rôle |
|-------------------|------|
| `slides.md` | Point d'entrée : couverture, sommaire, includes `src:`, slide de fin. |
| `pages/01..10.md` | Une section de contenu par fichier (cf. `presentation.md`). |
| `presentation.md` | Trame orale source (non incluse dans le diaporama). |
| `components/` | Composants Vue maison (style inspira-ui) : `AuroraBackground`, `GlowCard`, `StatCard`, `TechPill`, `Placeholder`. |
| `layouts/` | Layouts `cover` et `section` (fond aurora violet). |
| `styles/` | Thème global — couleurs **reprises de `web/src/styles.css`** (oklch). |
| `uno.config.ts` | Raccourcis UnoCSS (`cp-text-gradient`, `cp-card`, `cp-chip`). |
| `global-bottom.vue` | Pied de page persistant (logo + numéro de slide). |
| `public/` | Logo (`logo.svg`) et captures (`screenshots/`). |

## Fonctionnalités Slidev utilisées

- **Mermaid** pour les diagrammes (architecture, déploiement, sécurité, import, gamification, scaling).
- **Shiki** avec surlignage de lignes + **magic-move** (interface `StorageService` → Local → S3).
- **Composants Vue** custom pour les cartes, stats animées et fonds aurora.
- **KaTeX**, transitions, presenter mode — disponibles nativement.

## Captures d'écran

Les emplacements `<Placeholder>` affichent un cadre en pointillés tant qu'aucune
image n'est fournie. Pour les remplacer, déposez un PNG dans `public/screenshots/`
avec le nom attendu (ex. `swagger-ui.png`, `demo-web.png`, `demo-desktop.png`,
`demo-realtime.png`) — le placeholder se transforme automatiquement en image.
