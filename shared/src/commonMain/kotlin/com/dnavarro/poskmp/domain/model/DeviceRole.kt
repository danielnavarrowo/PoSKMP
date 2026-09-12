package com.dnavarro.poskmp.domain.model

/**
 * Define el rol y nivel de permisos del dispositivo/terminal en la red del punto de venta.
 */
enum class DeviceRole {
    /**
     * Caja Principal / Administrador:
     * Acceso total. Puede crear, modificar y sincronizar productos, ajustes de negocio,
     * clientes, cajeros y ventas.
     */
    ADMIN,

    /**
     * Caja Secundaria / Terminal POS:
     * Destinado exclusivamente a cobro. Puede registrar ventas, partidas y abonos de clientes.
     * No puede modificar ni subir cambios al catálogo de productos ni a los ajustes de negocio.
     */
    POS_CLIENT,

    /**
     * Checador / Pantalla de Consulta:
     * 100% Solo Lectura. Descarga periódicamente el catálogo y precios desde la nube,
     * pero nunca sube ningún dato al servidor.
     */
    CHECKER_ONLY
}
