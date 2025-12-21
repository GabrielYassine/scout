import { NavLink } from "react-router-dom";
import "./TopNavbar.css";

function NavItem({ to, label }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        isActive ? "nav-item active" : "nav-item"
      }
    >
      {label}
    </NavLink>
  );
}

export default function TopNavbar() {
  return (
    <header className="top-navbar">
      <div className="nav-left">Scout</div>

      <nav className="nav-center">
        <NavItem to="/" label="Home" />
        <NavItem to="/lab" label="Lab" />
        <NavItem to="/history" label="History" />
      </nav>
    </header>
  );
}
