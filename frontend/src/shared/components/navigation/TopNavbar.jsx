/**
 * Top navigation bar for main app routes.
 * @author s235257
 */

import { NavLink } from "react-router-dom";
import "@/shared/components/styles/TopNavbar.css";
import logo from "@/assets/icons/ScoutLogo.png";
import homeIcon from "@/assets/icons/Home.png";
import labIcon from "@/assets/icons/Lab.png";
import runIcon from "@/assets/icons/Run.png";

function NavItem({ to, label, icon, invertIcon = true }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        isActive ? "nav-item active" : "nav-item"
      }
    >
      {icon && (
        <img
          src={icon}
          alt=""
          className={invertIcon ? "nav-icon" : "nav-icon nav-icon--no-invert"}
        />
      )}
      {label}
    </NavLink>
  );
}

export default function TopNavbar() {
  return (
    <header className="top-navbar">
      <div className="nav-left">
        <span className="nav-title">SCOUT</span>
        <img src={logo} alt="SCOUT" className="nav-logo" />
      </div>

      <nav className="nav-center">
        <NavItem to="/" label="Home" icon={homeIcon} />
        <NavItem to="/lab" label="Lab" icon={labIcon} invertIcon={false} />
        <NavItem to="/run" label="Run" icon={runIcon} />
      </nav>
    </header>
  );
}
