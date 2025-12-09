-- Tabla de membresías
CREATE TABLE IF NOT EXISTS memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id VARCHAR(255) NOT NULL,
    operator_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'not_configured',
    
    -- Plan de membresía
    plan_amount DECIMAL(10, 2) NULL,
    plan_currency VARCHAR(3) NULL DEFAULT 'USD',
    plan_billing_day INTEGER NULL,
    plan_channel VARCHAR(50) NULL,
    plan_message TEXT NULL,
    
    -- Método de pago
    payment_method_brand VARCHAR(50) NULL,
    payment_method_last4 VARCHAR(4) NULL,
    payment_method_exp_month INTEGER NULL,
    payment_method_exp_year INTEGER NULL,
    payment_method_holder_name VARCHAR(255) NULL,
    payment_method_stripe_payment_method_id VARCHAR(255) NULL,
    
    -- Fechas
    next_charge_at TIMESTAMPTZ NULL,
    last_sent_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    -- Nota: La foreign key a operators se agregará cuando la tabla operators exista
    -- Por ahora, solo validamos que operator_id no sea NULL
    CONSTRAINT chk_membership_status CHECK (status IN ('not_configured', 'plan_draft', 'awaiting_payment', 'active', 'cancelled')),
    CONSTRAINT chk_billing_day CHECK (plan_billing_day IS NULL OR (plan_billing_day >= 1 AND plan_billing_day <= 28))
);

-- Índices para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_memberships_client_id ON memberships(client_id);
CREATE INDEX IF NOT EXISTS idx_memberships_operator_id ON memberships(operator_id);
CREATE INDEX IF NOT EXISTS idx_memberships_status ON memberships(status);
CREATE INDEX IF NOT EXISTS idx_memberships_next_charge_at ON memberships(next_charge_at) WHERE next_charge_at IS NOT NULL;

-- Tabla de operadores (si no existe)
-- Esta tabla se usará en el futuro para almacenar información de operadores
CREATE TABLE IF NOT EXISTS operators (
    id VARCHAR(255) PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    brand_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    country VARCHAR(2) NULL,
    currency VARCHAR(3) NULL DEFAULT 'USD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Agregar foreign key después de crear ambas tablas
-- (Comentado por ahora para evitar errores si operators no tiene datos)
-- ALTER TABLE memberships 
-- ADD CONSTRAINT fk_memberships_operator 
-- FOREIGN KEY (operator_id) REFERENCES operators(id) ON DELETE CASCADE;

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_memberships_updated_at
    BEFORE UPDATE ON memberships
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_operators_updated_at
    BEFORE UPDATE ON operators
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

