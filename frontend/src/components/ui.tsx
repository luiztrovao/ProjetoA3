// Elementos de tabela compartilhados por Cadastros, Vendas e Pedidos.

export function PersonCell({ initials, name, detail, image }: { initials: string; name: string; detail?: string; image?: string }) {
  return (
    <div className="entity-cell">
      <span className={`entity-avatar${image ? " has-image" : ""}`}>
        {image ? <img src={image} alt="" /> : initials}
      </span>
      <span>
        <strong>{name}</strong>
        {detail && <small>{detail}</small>}
      </span>
    </div>
  );
}

export function StatusBadge({ value }: { value: string }) {
  const statusClass =
    value === "Ativo" || value === "Disponível" || value === "Concluído"
      ? "success"
      : value === "Estoque baixo" || value === "Aguardando confirmação"
        ? "warning"
        : "inactive";
  return <span className={`management-badge ${statusClass}`}><i />{value}</span>;
}
