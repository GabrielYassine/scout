import { useAsyncResource } from "./useAsyncResource.js";
import { getCatalog } from "@/shared/api/catalog.js";

export function useCatalog() {
  const { data, loading, error } = useAsyncResource(() => getCatalog(), []);
  return { catalog: data, catalogLoading: loading, catalogError: error };
}
