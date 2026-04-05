import { RoutePlaceholder } from "@/components/layout/route-placeholder";
import { useI18n } from "@/i18n/locale-provider";

export function FreeAccessRoute() {
  const { locale } = useI18n();
  const copy =
    locale === "tr"
      ? {
          eyebrow: "Ücretsiz Erişim",
          title: "Erişim Anahtarı İzinleri",
          description:
            "Bu rota izin listesi, anahtar filtreleme ve iptal akışlarını barındıracak.",
          highlights: [
            "Varsayılan anahtar görünürlüğü",
            "İzin verme ve iptal etme aksiyonları",
            "İçeriğe bağlı erişim girişleri",
          ],
        }
      : {
          eyebrow: "Free Access",
          title: "Access Key Grants",
          description:
            "This route will host grant listing, key filtering, and revoke workflows.",
          highlights: [
            "Default key visibility",
            "Grant and revoke actions",
            "Content-linked access entries",
          ],
        };
  return (
    <RoutePlaceholder
      eyebrow={copy.eyebrow}
      title={copy.title}
      description={copy.description}
      highlights={copy.highlights}
    />
  );
}
