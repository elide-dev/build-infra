import { ExecutionContext } from "@cloudflare/workers-types";
import * as api from "../commons/api.mjs";
import auth from "../commons/auth.mjs";
import { errorResponse, handleErrors } from "../commons/handler.mjs";

// Handlers.
import stats from "./handlers/stats.mjs";
import query from "./handlers/query.mjs";

function versionedUrl(path: string): string {
  return `/${api.binstatService.name}/${api.binstatService.version}${path}`;
}

const routingTable = {
  // GET /devstat/v1/health - Check health.
  [`GET ${versionedUrl("/health")}`]: async () => new Response("ok"),

  // GET /devstat/v1/binstat - Query binary statistics by revision.
  [`GET ${versionedUrl(`/${api.binstatService.methods.binstat}`)}`]: query,

  // POST /devstat/v1/binstat - Receive binary statistics at build time.
  [`POST ${versionedUrl(`/${api.binstatService.methods.binstat}`)}`]: stats,
};

export default {
  async fetch(
    request: Request,
    env: Env,
    ctx: ExecutionContext,
  ): Promise<Response> {
    const url = new URL(request.url);
    if (!(await auth(request, env.SHARED_AUTH_TOKEN))) {
      return new Response("unauthorized", {
        status: 401,
        statusText: "Unauthorized",
      });
    }
    return await handleErrors(async () => {
      const handler = routingTable[`${request.method} ${url.pathname}`];
      if (!handler) {
        throw errorResponse("not found", 404);
      } else {
        return handler(request, env, ctx);
      }
    });
  },
};
