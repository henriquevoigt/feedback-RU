-- Script 02: processamento ativo do banco de dados.
-- A avaliação não pode ter sido enviada antes da entrada registrada na catraca.
-- O gatilho roda antes do INSERT/UPDATE e compara os dois horários no PostgreSQL.

CREATE OR REPLACE FUNCTION fn_validar_data_envio_feedback()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_data_hora_entrada TIMESTAMP;
BEGIN
    -- Permite que uma aplicação omita a data; o horário atual será registrado.
    IF NEW.data_envio IS NULL THEN
        NEW.data_envio := CURRENT_TIMESTAMP;
    END IF;

    SELECT c.data_hora_entrada
      INTO v_data_hora_entrada
      FROM consumo AS c
     WHERE c.id_consumo = NEW.id_consumo;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Não existe consumo com ID %.', NEW.id_consumo
            USING ERRCODE = '23503';
    END IF;

    IF NEW.data_envio < v_data_hora_entrada THEN
        RAISE EXCEPTION
            'A avaliação não pode ser enviada antes da entrada na catraca. Entrada: %, envio: %.',
            v_data_hora_entrada,
            NEW.data_envio
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_data_envio_feedback ON feedback;

CREATE TRIGGER trg_validar_data_envio_feedback
BEFORE INSERT OR UPDATE
ON feedback
FOR EACH ROW
EXECUTE FUNCTION fn_validar_data_envio_feedback();
