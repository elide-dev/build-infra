export default async function authorized(
  request: Request,
  env: Env,
): Promise<boolean> {
  if (!env.SHARED_AUTH_TOKEN) {
    throw new Error("no auth token configured");
  }
  const authHeader = request.headers.get("authorization");
  const tokenType = authHeader ? authHeader.split(" ")[0] : null;
  const tokenValue = authHeader ? authHeader.split(" ")[1] : null;
  if (tokenType === "Bearer" && !!tokenValue) {
    return tokenValue === env.SHARED_AUTH_TOKEN;
  }
  return false;
}
