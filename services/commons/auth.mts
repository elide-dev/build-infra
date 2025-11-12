export default async function authorized(
  request: Request,
  token: string,
): Promise<boolean> {
  if (!token) {
    throw new Error("no auth token configured");
  }
  const authHeader = request.headers.get("authorization");
  const tokenType = authHeader ? authHeader.split(" ")[0] : null;
  const tokenValue = authHeader ? authHeader.split(" ")[1] : null;
  if (tokenType === "Bearer" && !!tokenValue) {
    console.error(`Authorization token invalid; rejecting (length: ${tokenValue.length})`)
    return tokenValue === token
  }
  return false;
}
