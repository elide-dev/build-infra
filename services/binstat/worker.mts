import { ExecutionContext } from "@cloudflare/workers-types";
import * as api from "./api.mjs";
import auth from "./auth.mjs";
import stats from "./handlers/stats.mjs";
import { errorResponse, handleErrors } from "./handlers/base.mjs";

function versionedUrl(path: string): string {
  return `/${api.binstatService.name}/${api.binstatService.version}${path}`;
}

const routingTable = {
  // GET /binstat/v1/stat - Check health.
  [`GET ${versionedUrl("/stat")}`]: async () => new Response("ok"),

  // POST /binstat/v1/stat - Receive binary statistics at build time.
  [`POST ${versionedUrl("/stat")}`]: stats,
};

export default {
  async fetch(
    request: Request,
    env: Env,
    ctx: ExecutionContext,
  ): Promise<Response> {
    const url = new URL(request.url);
    if (!(await auth(request, env))) {
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
