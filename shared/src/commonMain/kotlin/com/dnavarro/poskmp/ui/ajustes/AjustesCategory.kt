package com.dnavarro.poskmp.ui.ajustes

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import poskmp.shared.generated.resources.Res
import poskmp.shared.generated.resources.analytics
import poskmp.shared.generated.resources.info
import poskmp.shared.generated.resources.light_mode
import poskmp.shared.generated.resources.payments
import poskmp.shared.generated.resources.person
import poskmp.shared.generated.resources.point_of_sale
import poskmp.shared.generated.resources.restore
import poskmp.shared.generated.resources.settings
import poskmp.shared.generated.resources.settings_cat_about_subtitle
import poskmp.shared.generated.resources.settings_cat_about_title
import poskmp.shared.generated.resources.settings_cat_appearance_subtitle
import poskmp.shared.generated.resources.settings_cat_appearance_title
import poskmp.shared.generated.resources.settings_cat_backup_subtitle
import poskmp.shared.generated.resources.settings_cat_backup_title
import poskmp.shared.generated.resources.settings_cat_cashiers_subtitle
import poskmp.shared.generated.resources.settings_cat_cashiers_title
import poskmp.shared.generated.resources.settings_cat_clientes_subtitle
import poskmp.shared.generated.resources.settings_cat_clientes_title
import poskmp.shared.generated.resources.settings_cat_general_subtitle
import poskmp.shared.generated.resources.settings_cat_general_title
import poskmp.shared.generated.resources.settings_cat_pricing_subtitle
import poskmp.shared.generated.resources.settings_cat_pricing_title
import poskmp.shared.generated.resources.settings_cat_sync_subtitle
import poskmp.shared.generated.resources.settings_cat_sync_title
import poskmp.shared.generated.resources.settings_cat_ticket_subtitle
import poskmp.shared.generated.resources.settings_cat_ticket_title
import poskmp.shared.generated.resources.settings_cat_ventas_subtitle
import poskmp.shared.generated.resources.settings_cat_ventas_title
import poskmp.shared.generated.resources.sync

enum class AjustesCategory(
    val titleRes: StringResource,
    val subtitleRes: StringResource,
    val icon: DrawableResource
) {
    CLIENTES(
        titleRes = Res.string.settings_cat_clientes_title,
        subtitleRes = Res.string.settings_cat_clientes_subtitle,
        icon = Res.drawable.person
    ),
    VENTAS(
        titleRes = Res.string.settings_cat_ventas_title,
        subtitleRes = Res.string.settings_cat_ventas_subtitle,
        icon = Res.drawable.analytics
    ),
    GENERAL(
        titleRes = Res.string.settings_cat_general_title,
        subtitleRes = Res.string.settings_cat_general_subtitle,
        icon = Res.drawable.settings
    ),
    APARIENCIA(
        titleRes = Res.string.settings_cat_appearance_title,
        subtitleRes = Res.string.settings_cat_appearance_subtitle,
        icon = Res.drawable.light_mode
    ),
    PRECIOS_MARGENES(
        titleRes = Res.string.settings_cat_pricing_title,
        subtitleRes = Res.string.settings_cat_pricing_subtitle,
        icon = Res.drawable.payments
    ),
    TICKET_IMPRESORA(
        titleRes = Res.string.settings_cat_ticket_title,
        subtitleRes = Res.string.settings_cat_ticket_subtitle,
        icon = Res.drawable.point_of_sale
    ),
    CAJEROS(
        titleRes = Res.string.settings_cat_cashiers_title,
        subtitleRes = Res.string.settings_cat_cashiers_subtitle,
        icon = Res.drawable.person
    ),
    SINCRONIZACION(
        titleRes = Res.string.settings_cat_sync_title,
        subtitleRes = Res.string.settings_cat_sync_subtitle,
        icon = Res.drawable.sync
    ),
    COPIA_SEGURIDAD(
        titleRes = Res.string.settings_cat_backup_title,
        subtitleRes = Res.string.settings_cat_backup_subtitle,
        icon = Res.drawable.restore
    ),
    SISTEMA_ACERCA_DE(
        titleRes = Res.string.settings_cat_about_title,
        subtitleRes = Res.string.settings_cat_about_subtitle,
        icon = Res.drawable.info
    )
}
