import { ZodError } from "zod";

export function errorResponse(
  message: string | null = null,
  status: number = 400,
): never {
  throw new Response(message || null, { status });
}

export async function handleErrors(
  handler: () => Promise<Response>,
): Promise<Response> {
  try {
    return await handler();
  } catch (err) {
    if (err instanceof Response) {
      console.log("Known error case; responding", err);
      return err;
    } else if (err instanceof ZodError) {
      console.error("Validation error case; responding 400", {
        issues: err.issues,
      });

      const headers = new Headers();
      headers.set("content-type", "application/json");

      return new Response(
        JSON.stringify(
          { error: "invalid payload", issues: err.issues },
          null,
          2,
        ),
        {
          status: 400,
          headers,
        },
      );
    } else {
      console.error("Unknown error case; responding 500", err);
      return new Response("internal server error", { status: 500 });
    }
  }
}

export async function decodeJson<T>(request: Request): Promise<T> {
  try {
    return (await request.json()) as T;
  } catch {
    errorResponse("invalid json", 400);
  }
}

export async function decodeJsonAndValidate<T>(
  request: Request,
  validator: (data: any) => T,
): Promise<T> {
  const data = await decodeJson<unknown>(request);
  return validator(data);
}
