import { RoutePlaceholder } from "@/components/layout/route-placeholder";
import { useI18n } from "@/i18n/locale-provider";

export function MediaProcessingRoute() {
  const { locale } = useI18n();

  return (
    <RoutePlaceholder
      eyebrow={locale === "tr" ? "Medya İşleme" : "Media Processing"}
      title={locale === "tr" ? "İşleme Konsolu" : "Processing Console"}
      description={
        locale === "tr"
          ? "Bu rota işlem işi sorgusu, durum görünümü ve planlama/yeniden deneme kontrollerini barındıracak."
          : "This route will host processing job lookup, status views, and schedule/retry controls."
      }
      highlights={
        locale === "tr"
          ? [
              "Son iş geçmişi",
              "Yerelleştirme durum sorgusu",
              "Planlama ve yeniden deneme akışları",
            ]
          : [
              "Recent job history",
              "Localization status lookup",
              "Schedule and retry workflows",
            ]
      }
    />
  );
}
