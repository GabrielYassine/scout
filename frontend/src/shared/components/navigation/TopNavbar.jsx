/**
  *
  * @author s235257
 */

import { NavLink } from "react-router-dom";
import "@/shared/components/styles/TopNavbar.css";
import logo from "@/assets/icons/ScoutLogo.png";
import homeIcon from "@/assets/icons/Home.png";
import labIcon from "@/assets/icons/Lab.png";

function NavItem({ to, label, icon }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        isActive ? "nav-item active" : "nav-item"
      }
    >
      {icon && <img src={icon} alt="" className="nav-icon" />}
      {label}
    </NavLink>
  );
}

export default function TopNavbar() {
  return (
    <header className="top-navbar">
      <div className="nav-left">
        <img src={logo} alt="Scout" className="nav-logo" />
        Scout
      </div>

      <nav className="nav-center">
        <NavItem to="/" label="Home" icon={homeIcon} />
        <NavItem to="/lab" label="Lab" icon={labIcon} />
        <NavItem to="/run" label="Run" icon={labIcon} />
      </nav>
    </header>
  );
}
