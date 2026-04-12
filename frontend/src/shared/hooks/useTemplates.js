import { useAsyncResource } from "./useAsyncResource.js";
import { getTemplates } from "@/shared/api/templates.js";

export function useTemplates() {
  const { data, loading, error } = useAsyncResource(() => getTemplates(), []);
  return { templates: data ?? [], templatesLoading: loading, templatesError: error };
}
