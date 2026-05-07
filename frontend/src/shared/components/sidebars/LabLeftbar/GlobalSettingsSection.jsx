/**
  *
  * @author s235257 & s230632
 */
 
import SidebarSection from "../SidebarSection.jsx";
import ParamField from "../ParamField.jsx";

export default function GlobalSettingsSection({
  open,
  setOpen,
  disabled,
  runMode,
  params,
  onModeChange,
  setParam,
}) {
  return (
    <SidebarSection
      title="Global Settings"
      isOpen={open.global}
      onToggle={() => setOpen((current) => ({ ...current, global: !current.global }))}
    >
      <div className="ll-subsection">
        <div className="field-row">
          <label className="field-label">Mode</label>
          <select
            className="field-input"
            disabled={disabled}
            value={runMode}
            onChange={(e) => onModeChange(e.target.value)}
          >
            <option value="run">Standard Run</option>
            <option value="runtimeStudy">Runtime Study</option>
          </select>
        </div>

        <ParamField
          def={{
            key: "seed",
            label: "Seed",
            type: "long",
            min: 1,
          }}
          disabled={disabled}
          value={params.global?.seed}
          onValueChange={(value) =>
            setParam("global", { key: "seed", type: "long" }, value)
          }
        />

        {runMode === "run" && (
          <ParamField
            def={{
              key: "runTimes",
              label: "Run Times",
              type: "int",
              min: 1,
            }}
            disabled={disabled}
            value={params.global?.runTimes}
            onValueChange={(value) =>
              setParam("global", { key: "runTimes", type: "int" }, value)
            }
          />
        )}

        {runMode === "runtimeStudy" && (
          <ParamField
            def={{
              key: "repetitionsPerSize",
              label: "Repetitions per Size",
              type: "int",
              min: 1,
            }}
            disabled={disabled}
            value={params.global?.repetitionsPerSize}
            onValueChange={(value) =>
              setParam("global", { key: "repetitionsPerSize", type: "int" }, value)
            }
          />
        )}

        {runMode === "run" && (
          <ParamField
            def={{
              key: "logEveryIterations",
              label: "Backend log every X iterations",
              type: "int",
              min: 10,
            }}
            disabled={disabled}
            value={params.global?.logEveryIterations}
            onValueChange={(value) =>
              setParam("global", { key: "logEveryIterations", type: "int" }, value)
            }
          />
        )}

        {runMode === "run" && (
          <ParamField
            def={{
              key: "wsUpdateEveryIterations",
              label: "WebSocket update every X iterations",
              type: "int",
              min: 1,
            }}
            disabled={disabled}
            value={params.global?.wsUpdateEveryIterations}
            onValueChange={(value) =>
              setParam(
                "global",
                { key: "wsUpdateEveryIterations", type: "int" },
                value
              )
            }
          />
        )}

        {runMode === "runtimeStudy" && (
          <ParamField
            def={{
              key: "problemSizes",
              label: "Problem Sizes (comma separated)",
              type: "string",
            }}
            disabled={disabled}
            value={params.global?.problemSizes}
            onValueChange={(value) =>
              setParam("global", { key: "problemSizes", type: "string" }, value)
            }
          />
        )}
      </div>
    </SidebarSection>
  );
}