import { ExecutionContext } from "@cloudflare/workers-types";
import * as api from "../commons/api.mjs";
import auth from "../commons/auth.mjs";
import stats from "./handlers/reports.mjs";
import { errorResponse, handleErrors } from "../commons/handler.mjs";

function versionedUrl(path: string): string {
  return `/${api.reportService.name}/${api.reportService.version}${path}`;
}

const routingTable = {
  // GET /reports/v1/health - Check health.
  [`GET ${versionedUrl("/health")}`]: async () => new Response("ok"),

  // POST /reports/v1/submit - Receive dev-time reports.
  [`POST ${versionedUrl("/submit")}`]: stats,
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
