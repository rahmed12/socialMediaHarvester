// The sidebar groups. Later tickets add items, shown or hidden by role.
const GROUPS = [
  { name: 'Data', items: [] },
  { name: 'Operate', items: [] },
  { name: 'Admin', items: [] },
]

export default function Sidebar() {
  return (
    <nav aria-label="Sidebar" className="sidebar">
      {GROUPS.map((group) => {
        const headingId = `sidebar-${group.name.toLowerCase()}`
        return (
          <section key={group.name} aria-labelledby={headingId}>
            <h2 id={headingId}>{group.name}</h2>
            <ul>
              {group.items.map((item) => <li key={item.label}>{item.label}</li>)}
            </ul>
          </section>
        )
      })}
    </nav>
  )
}
