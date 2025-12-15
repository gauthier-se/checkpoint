# Build stage
FROM node:25-alpine AS builder

RUN npm install -g pnpm

WORKDIR /app

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

RUN pnpm install --frozen-lockfile

COPY . .

# Build the application
RUN pnpm run build

FROM node:25-alpine AS runner

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create a non-root user for more security
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nodejs -u 1001

WORKDIR /app

COPY --from=builder --chown=nodejs:nodejs /app/.output /app/.output

COPY --from=builder --chown=nodejs:nodejs /app/package.json /app/package.json

USER nodejs

EXPOSE 3000

ENV NODE_ENV=production \
    PORT=3000

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

CMD ["node", ".output/server/index.mjs"]
